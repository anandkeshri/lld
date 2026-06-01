package org.example.slice.eventTicketBookingSystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.slice.eventTicketBookingSystem.dto.BookingRequest;
import org.example.slice.eventTicketBookingSystem.dto.BookingResponse;
import org.example.slice.eventTicketBookingSystem.model.OrderStatus;
import org.example.slice.eventTicketBookingSystem.service.BookingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Concurrency test that verifies the booking system prevents overselling
 * under high parallel load.
 *
 * ── Scenario ────────────────────────────────────────────────────────────────
 * An event is created with exactly CAPACITY = 5 VIP seats.
 * THREADS = 20 concurrent requests all attempt to book 1 seat at the same
 * moment.  Only 5 should succeed; the remaining 15 must be gracefully
 * rejected.  The final available_capacity must be exactly 0 — never negative.
 *
 * ── Why the concurrent calls bypass MockMvc ──────────────────────────────────
 * concurrent MockMvc.perform() calls all flow through the same
 * OpenEntityManagerInViewInterceptor instance.  The interceptor stores the
 * EntityManager in thread-local storage, but the same EntityManager factory
 * state (first-level cache, session context) can interfere across threads in
 * the MOCK web environment, causing transactions to see stale state before
 * the SELECT FOR UPDATE lock can serialise them.
 *
 * Calling bookingService.bookTicket() directly from each thread bypasses the
 * servlet stack entirely.  There is no open-in-view EntityManager pre-bound
 * to the thread, so each @Transactional method starts with a completely clean
 * context, gets a fresh EntityManager, issues its own SELECT FOR UPDATE, and
 * commits independently.  This is the environment where the DB-level lock
 * does exactly what it is designed to do.
 *
 * Setup and teardown still use MockMvc (creating the event, setting capacity,
 * verifying capacity at the end) because that part of the test is
 * sequential and exercises the REST layer correctly.
 *
 * ── Why CountDownLatch as a "start gun" ─────────────────────────────────────
 * Without a barrier, threads start staggered by scheduling jitter.  A
 * startGun latch holds all THREADS workers at a rendezvous point and fires
 * them simultaneously, maximising the chance that their DB transactions
 * overlap and create genuine lock contention — the exact condition that the
 * SELECT FOR UPDATE in SeatInventoryService.reserveSeats() must serialise.
 *
 * ── Why PESSIMISTIC booking strategy ────────────────────────────────────────
 * PESSIMISTIC acquires a SELECT FOR UPDATE row-level lock on seat_inventory
 * before decrementing available_capacity.  Only one transaction can hold the
 * lock at a time; all others block until the holder commits.  After each
 * commit the next waiter re-reads the authoritative value from the DB — so
 * the count decrements correctly 5 → 4 → 3 → 2 → 1 → 0, and the 6th
 * through 20th callers see 0 and are rejected.
 *
 * ── Cleanup ──────────────────────────────────────────────────────────────────
 * JdbcTemplate is used for teardown: a single SQL statement keyed on event_id
 * cleans all created rows regardless of how many threads succeeded or failed.
 *
 * Requires a running MySQL instance configured in application.properties.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingConcurrencyTest {

    // ── Configuration ────────────────────────────────────────────────────────

    /** Total VIP seats — intentionally small to guarantee exhaustion. */
    private static final int    CAPACITY  = 5;

    /**
     * Number of simultaneous booking attempts.
     * 4× CAPACITY ensures contention; all should complete quickly.
     */
    private static final int    THREADS   = 20;

    private static final String SEAT_TYPE  = "VIP";
    private static final double UNIT_PRICE = 1000.00;

    /** Unique event name per run — prevents collision when test is re-run. */
    private final String EVENT_NAME = "Concurrency Test " + System.currentTimeMillis();

    // ── Infrastructure ───────────────────────────────────────────────────────

    /**
     * Used for setup (create event, set capacity) and final capacity check.
     * NOT used for the concurrent booking calls — see class-level comment.
     */
    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;

    /**
     * Injected directly so concurrent threads can call bookTicket() without
     * going through the servlet stack or open-in-view interceptor.
     */
    @Autowired private BookingService bookingService;

    /** Used only for teardown via raw SQL; handles FK ordering cleanly. */
    @Autowired private JdbcTemplate jdbcTemplate;

    // ── Shared state ─────────────────────────────────────────────────────────
    private Long eventId;

    // ── Setup ────────────────────────────────────────────────────────────────

    /**
     * Creates the event, opens the sale window, and sets VIP capacity via
     * the REST API.  Runs once before the test.
     */
    @BeforeAll
    void setUpEventAndInventory() throws Exception {

        // 1. Create event with a FUTURE sale start (sale will be patched open next)
        MvcResult createResult = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":          "%s",
                                  "description":   "Concurrency test — safe to delete",
                                  "scheduledTime": "2027-12-31T20:00:00",
                                  "venue":         "Load Test Arena",
                                  "seatTypes":     ["%s"],
                                  "saleStartTime": "2027-10-01T00:00:00",
                                  "saleEndTime":   "2027-12-30T23:59:00"
                                }
                                """.formatted(EVENT_NAME, SEAT_TYPE)))
                .andExpect(status().isCreated())
                .andReturn();

        eventId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 2. Open sale window immediately
        mockMvc.perform(patch("/api/events/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "saleStartTime": "2026-01-01T00:00:00" }
                                """))
                .andExpect(status().isOk());

        // 3. Set VIP capacity — this is the scarce resource all threads contend for
        mockMvc.perform(put("/api/events/" + eventId + "/inventory/" + SEAT_TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "originalCapacity": %d, "pricing": %.2f }
                                """.formatted(CAPACITY, UNIT_PRICE)))
                .andExpect(status().isOk());
    }

    // ── Teardown ─────────────────────────────────────────────────────────────

    /**
     * Deletes all rows created during the test in FK-safe order:
     *   tickets → CANCEL orders → BOOK orders → seat_inventory → event_seat_types → events
     */
    @AfterAll
    void tearDown() {
        if (eventId == null) return;
        jdbcTemplate.update("DELETE FROM tickets   WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM orders    WHERE event_id = ? AND reference_order_id IS NOT NULL", eventId);
        jdbcTemplate.update("DELETE FROM orders    WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM seat_inventory  WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM event_seat_types WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM events    WHERE id = ?", eventId);
    }

    // ── Test ─────────────────────────────────────────────────────────────────

    /**
     * Fires THREADS simultaneous bookingService.bookTicket() calls and asserts:
     *
     *  1. Exactly CAPACITY responses have status TICKETS_ISSUED.
     *  2. Exactly THREADS - CAPACITY responses have status SEATS_UNAVAILABLE.
     *  3. No thread threw an unexpected exception.
     *  4. Final available_capacity = 0 (confirmed via the REST capacity endpoint).
     *  5. soldCount = CAPACITY (inventory audit matches the successful bookings).
     */
    @Test
    @DisplayName(THREADS + " concurrent bookings for " + CAPACITY
            + " seats — exactly " + CAPACITY + " succeed, 0 oversold")
    void concurrentBookings_noOverselling() throws Exception {

        /*
         * startGun: initialised to 1.
         * Every worker calls startGun.await() right after submission.
         * A single countDown() from the main thread releases all workers
         * simultaneously, maximising lock contention at the DB layer.
         */
        CountDownLatch startGun = new CountDownLatch(1);

        /*
         * doneLatch: counts down once per completed worker.
         * Main thread blocks on doneLatch.await() until all THREADS workers
         * have finished their bookTicket() call and stored their result.
         */
        CountDownLatch doneLatch = new CountDownLatch(THREADS);

        /*
         * CopyOnWriteArrayList provides thread-safe add() without
         * external synchronisation — needed because THREADS workers
         * write to these lists concurrently.
         */
        List<OrderStatus> results = new CopyOnWriteArrayList<>();
        List<Throwable>   errors  = new CopyOnWriteArrayList<>();

        /*
         * Fixed pool sized to THREADS ensures all workers run concurrently
         * without queuing behind each other in the executor.
         */
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        // ── Submit all workers ───────────────────────────────────────────────
        for (int i = 0; i < THREADS; i++) {
            final String userId = "concurrent-user-" + i;

            executor.submit(() -> {
                try {
                    startGun.await(); // block here until the main thread fires

                    BookingRequest request = new BookingRequest();
                    request.setUserId(userId);
                    request.setEventId(eventId);
                    request.setSeatType(SEAT_TYPE);
                    request.setQuantity(1);
                    request.setBookingStrategy("PESSIMISTIC");
                    request.setPricingStrategy("FIXED");

                    /*
                     * Direct service call — no servlet stack, no open-in-view.
                     * Each thread creates a clean @Transactional context,
                     * its own EntityManager, and issues SELECT FOR UPDATE.
                     * The DB lock correctly serialises these calls.
                     */
                    BookingResponse response = bookingService.bookTicket(request);
                    results.add(response.getStatus());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add(e);
                } catch (Exception e) {
                    // Capture all unexpected exceptions for assertion below
                    errors.add(e);
                } finally {
                    doneLatch.countDown(); // always signal completion
                }
            });
        }

        // ── Fire all threads simultaneously ──────────────────────────────────
        startGun.countDown();

        // ── Wait for all threads to finish ───────────────────────────────────
        boolean allFinished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // ── Assertions ───────────────────────────────────────────────────────

        assertTrue(allFinished,
                "All " + THREADS + " threads must complete within 30 seconds");

        assertTrue(errors.isEmpty(),
                "No unexpected exceptions should occur. Errors: " + errors);

        assertEquals(THREADS, results.size(),
                "Every thread must record exactly one result");

        // Count successful bookings (seat lock acquired, ticket issued)
        long successCount = results.stream()
                .filter(s -> s == OrderStatus.TICKETS_ISSUED)
                .count();

        // Count clean rejections (lock acquired but no seats left)
        long rejectedCount = results.stream()
                .filter(s -> s == OrderStatus.SEATS_UNAVAILABLE)
                .count();

        // Core assertion 1 — no overbooking: exactly CAPACITY seats sold
        assertEquals(CAPACITY, successCount,
                "Exactly " + CAPACITY + " bookings should succeed. "
                + "More means overbooking. Actual statuses: " + results);

        // Core assertion 2 — every rejection was a clean SEATS_UNAVAILABLE, not an error
        assertEquals(THREADS - CAPACITY, rejectedCount,
                "All remaining " + (THREADS - CAPACITY)
                + " requests should return SEATS_UNAVAILABLE. Actual: " + results);

        // ── Verify final inventory state via the capacity endpoint ────────────

        MvcResult capacityResult = mockMvc.perform(get("/api/events/" + eventId + "/capacity"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode capacityArray = objectMapper.readTree(
                capacityResult.getResponse().getContentAsString());

        JsonNode vipEntry = null;
        for (JsonNode node : capacityArray) {
            if (SEAT_TYPE.equals(node.get("seatType").asText())) {
                vipEntry = node;
                break;
            }
        }
        assertNotNull(vipEntry, "VIP entry must appear in the capacity response");

        // Core assertion 3 — available capacity is exactly 0, never negative
        assertEquals(0, vipEntry.get("availableCapacity").asInt(),
                "availableCapacity must be exactly 0 after selling out — never negative");

        // Core assertion 4 — sold count equals original capacity
        assertEquals(CAPACITY, vipEntry.get("soldCount").asInt(),
                "soldCount must equal original capacity — confirms no overbooking in the DB");

        // Core assertion 5 — original capacity unchanged by booking operations
        assertEquals(CAPACITY, vipEntry.get("originalCapacity").asInt(),
                "originalCapacity must not be modified by booking operations");
    }
}
