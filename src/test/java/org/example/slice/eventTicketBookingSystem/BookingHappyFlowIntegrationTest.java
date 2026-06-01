package org.example.slice.eventTicketBookingSystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.slice.eventTicketBookingSystem.repository.OrderRepository;
import org.example.slice.eventTicketBookingSystem.repository.TicketRepository;
import org.example.slice.eventTicketBookingSystem.repository.EventRepository;
import org.example.slice.eventTicketBookingSystem.repository.SeatInventoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end happy flow integration test covering the full booking lifecycle:
 *
 *  Step 1  — Create event with a FUTURE sale start date
 *  Step 2  — Set VIP seat capacity and pricing
 *  Step 3  — Book → must FAIL (sale window not open)
 *  Step 4  — Patch sale start time to a past date
 *  Step 5  — Book again → must SUCCEED (2 tickets issued)
 *  Step 6  — Verify seat capacity has decreased
 *  Step 7  — Cancel the first ticket
 *  Step 8  — Verify history has both BOOK and CANCEL orders
 *  Step 9  — Verify the cancelled ticket is in CANCELLED status in history
 *
 * ── Why @TestInstance(PER_CLASS) ────────────────────────────────────────────
 * By default JUnit 5 creates a new test instance per method, so instance
 * fields (eventId, ticketId, …) would reset between steps and the flow
 * would break.  PER_CLASS keeps a single instance for the whole class so
 * fields captured in one step are available in later steps.  It also lets
 * @AfterAll be non-static, which avoids the parameter-injection edge cases
 * that come with static lifecycle methods + Spring @Autowired.
 *
 * ── Why @TestMethodOrder(OrderAnnotation) ───────────────────────────────────
 * JUnit 5 does not guarantee alphabetical or declaration order without an
 * explicit ordering strategy.  @Order(n) on each method combined with
 * MethodOrderer.OrderAnnotation ensures the steps always execute 1 → 9.
 *
 * Requires a running MySQL instance configured in application.properties.
 * @AfterAll deletes all test data so the test is safe to re-run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)   // one shared instance → fields persist across steps
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // steps run in @Order(n) sequence
class BookingHappyFlowIntegrationTest {

    // ── Constants ────────────────────────────────────────────────────────────
    private static final String SEAT_TYPE  = "VIP";
    private static final int    CAPACITY   = 100;
    private static final double UNIT_PRICE = 5000.00;
    private static final int    QUANTITY   = 2;

    // Unique per run — prevents collisions when the test is re-run against the
    // same DB (e.g. after a mid-run failure that skipped @AfterAll cleanup).
    private final String USER_ID     = "happy-flow-user-" + System.currentTimeMillis();
    private final String EVENT_NAME  = "Happy Flow Concert "  + System.currentTimeMillis();

    // ── State shared across steps ────────────────────────────────────────────
    // Instance fields (not static) — safe because @TestInstance(PER_CLASS)
    // guarantees a single object for the lifetime of this test class.
    private Long eventId;           // captured in step 1, used in steps 2-9
    private Long bookOrderId;       // captured in step 5, verified in step 8
    private Long cancelledTicketId; // captured in step 5, cancelled in step 7, verified in step 9

    // ── Spring beans ─────────────────────────────────────────────────────────
    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;

    // Repositories used only for cleanup in @AfterAll
    @Autowired private TicketRepository        ticketRepository;
    @Autowired private OrderRepository         orderRepository;
    @Autowired private SeatInventoryRepository seatInventoryRepository;
    @Autowired private EventRepository         eventRepository;

    // ── Cleanup ──────────────────────────────────────────────────────────────

    /**
     * Deletes every record created during this test in safe cascade order:
     * tickets → orders → seat_inventory → event_seat_types → event.
     * Runs once after all 9 steps finish.
     */
    @AfterAll
    void cleanUp() {
        // Delete tickets belonging to the book order
        if (bookOrderId != null) {
            ticketRepository.findByOrderId(bookOrderId)
                    .forEach(t -> ticketRepository.deleteById(t.getId()));
        }
        // Delete orders respecting the FK: CANCEL orders hold a reference_order_id
        // pointing to the BOOK order, so CANCEL orders must be deleted first.
        var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(USER_ID);
        orders.stream().filter(o -> o.getReferenceOrderId() != null)
              .forEach(orderRepository::delete);   // CANCEL orders first
        orders.stream().filter(o -> o.getReferenceOrderId() == null)
              .forEach(orderRepository::delete);   // BOOK orders second
        // Delete seat inventory rows and the event itself.
        // deleteByEventId uses @Modifying @Query which requires a caller-side
        // transaction — not available in @AfterAll.  findByEventId + delete(entity)
        // works because JpaRepository.delete() carries its own @Transactional.
        if (eventId != null) {
            seatInventoryRepository.findByEventId(eventId)
                    .forEach(seatInventoryRepository::delete);
            eventRepository.deleteById(eventId);
        }
    }

    // ── Test steps ───────────────────────────────────────────────────────────

    /**
     * STEP 1 — Create an event with a FUTURE sale start date.
     *
     * The sale window opens in 2027 so that any booking attempt before
     * Step 4's patch is correctly rejected with SaleNotActiveException.
     */
    @Test
    @Order(1)
    @DisplayName("Step 1 — Create event with future sale start date")
    void step1_createEvent() throws Exception {

        String body = """
                {
                  "name":          "%s",
                  "description":   "Integration test — safe to delete",
                  "scheduledTime": "2027-12-31T20:00:00",
                  "venue":         "Test Arena, Bangalore",
                  "seatTypes":     ["VIP", "GENERAL"],
                  "saleStartTime": "2027-10-01T00:00:00",
                  "saleEndTime":   "2027-12-30T23:59:00"
                }
                """.formatted(EVENT_NAME);

        MvcResult result = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(EVENT_NAME))
                .andExpect(jsonPath("$.seatTypes", hasItems("VIP", "GENERAL")))
                .andReturn();

        // Capture the event ID — all subsequent steps reference this
        eventId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        assertNotNull(eventId, "Event ID must be assigned by the database");
    }

    /**
     * STEP 2 — Set VIP seat capacity and base pricing.
     *
     * On event creation, seat_inventory rows are initialised with capacity = 0.
     * This step sets the real values before sales open.
     */
    @Test
    @Order(2)
    @DisplayName("Step 2 — Set VIP capacity = 100 @ ₹5000")
    void step2_setSeatCapacity() throws Exception {

        mockMvc.perform(put("/api/events/" + eventId + "/inventory/" + SEAT_TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "originalCapacity": %d, "pricing": %.2f }
                                """.formatted(CAPACITY, UNIT_PRICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalCapacity").value(CAPACITY))
                .andExpect(jsonPath("$.availableCapacity").value(CAPACITY))  // fully available at start
                .andExpect(jsonPath("$.pricing").value(UNIT_PRICE));
    }

    /**
     * STEP 3 — Attempt to book while the sale window is CLOSED.
     *
     * BookingService.validateSaleWindow() compares now() against
     * saleStartTime / saleEndTime and throws SaleNotActiveException → HTTP 400.
     */
    @Test
    @Order(3)
    @DisplayName("Step 3 — Booking FAILS: sale window not open yet")
    void step3_bookingFailsBecauseSaleNotActive() throws Exception {

        mockMvc.perform(post("/api/bookings/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBookingRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Sale Not Active"))
                .andExpect(jsonPath("$.message", containsString("not currently active")));
    }

    /**
     * STEP 4 — Patch saleStartTime to a past date, opening the sale window.
     *
     * Only saleStartTime is sent in the body; all other event fields stay unchanged.
     * This exercises the PATCH (partial update) endpoint added to EventController.
     */
    @Test
    @Order(4)
    @DisplayName("Step 4 — Patch saleStartTime to past, opening the sale")
    void step4_patchSaleStartTime() throws Exception {

        mockMvc.perform(patch("/api/events/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "saleStartTime": "2026-01-01T00:00:00" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleStartTime").value("2026-01-01T00:00:00"));
    }

    /**
     * STEP 5 — Book 2 VIP tickets now that the sale is open.
     *
     * Strategy: PESSIMISTIC — DB row lock is acquired, capacity decremented,
     *           then payment runs, then tickets are issued.
     * Pricing:  FIXED — unit price = base price stored in seat_inventory.
     *
     * Captures bookOrderId and cancelledTicketId for later steps.
     */
    @Test
    @Order(5)
    @DisplayName("Step 5 — Booking SUCCEEDS: 2 VIP tickets issued")
    void step5_bookingSucceeds() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/bookings/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBookingRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TICKETS_ISSUED"))
                .andExpect(jsonPath("$.orderType").value("BOOK"))
                .andExpect(jsonPath("$.quantity").value(QUANTITY))
                .andExpect(jsonPath("$.unitPrice").value(UNIT_PRICE))
                .andExpect(jsonPath("$.totalAmount").value(QUANTITY * UNIT_PRICE))
                .andExpect(jsonPath("$.tickets", hasSize(QUANTITY)))
                .andExpect(jsonPath("$.tickets[0].status").value("ISSUED"))
                .andExpect(jsonPath("$.tickets[1].status").value("ISSUED"))
                .andExpect(jsonPath("$.tickets[0].ticketNumber", startsWith("TKT-")))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        bookOrderId       = response.get("orderId").asLong();
        cancelledTicketId = response.get("tickets").get(0).get("ticketId").asLong();

        assertNotNull(bookOrderId,       "Book order ID must be present");
        assertNotNull(cancelledTicketId, "First ticket ID must be present");
    }

    /**
     * STEP 6 — Verify seat capacity has decreased after the booking.
     *
     * We booked 2 of 100 seats, so:
     *   availableCapacity = 98   (100 - 2)
     *   soldCount         = 2
     */
    @Test
    @Order(6)
    @DisplayName("Step 6 — Seat capacity reduced: available=98, sold=2")
    void step6_verifySeatCapacityReduced() throws Exception {

        mockMvc.perform(get("/api/events/" + eventId + "/capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.seatType == 'VIP')].originalCapacity",
                        contains(CAPACITY)))
                .andExpect(jsonPath("$[?(@.seatType == 'VIP')].availableCapacity",
                        contains(CAPACITY - QUANTITY)))
                .andExpect(jsonPath("$[?(@.seatType == 'VIP')].soldCount",
                        contains(QUANTITY)));
    }

    /**
     * STEP 7 — Cancel the first of the two issued tickets.
     *
     * Side effects verified in later steps:
     *   - A CANCEL order is created, linked to the BOOK order via referenceOrderId
     *   - availableCapacity increments back by 1 (98 → 99)
     *   - The ticket's status changes from ISSUED → CANCELLED
     */
    @Test
    @Order(7)
    @DisplayName("Step 7 — Cancel first ticket: expect 204")
    void step7_cancelTicket() throws Exception {

        mockMvc.perform(post("/api/bookings/cancel/" + cancelledTicketId))
                .andExpect(status().isNoContent());
    }

    /**
     * STEP 8 — Fetch booking history and verify the order audit trail.
     *
     * After one book and one cancel, history must contain exactly 2 orders:
     *   - 1 BOOK  order in TICKETS_ISSUED status
     *   - 1 CANCEL order in REFUND_COMPLETED status with referenceOrderId set
     *
     * Note: referenceOrderId is compared as String to avoid int/long
     * mismatch in JSONPath (Jayway returns Integer for small JSON numbers).
     */
    @Test
    @Order(8)
    @DisplayName("Step 8 — History has 1 BOOK + 1 CANCEL order")
    void step8_verifyBookingHistory() throws Exception {

        mockMvc.perform(get("/api/bookings/history").param("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.orderType == 'BOOK')]",   hasSize(1)))
                .andExpect(jsonPath("$[?(@.orderType == 'CANCEL')]", hasSize(1)))
                // CANCEL order status must be REFUND_COMPLETED
                .andExpect(jsonPath("$[?(@.orderType == 'CANCEL')].status",
                        hasItem("REFUND_COMPLETED")))
                // CANCEL order must reference the original BOOK order (not null)
                .andExpect(jsonPath("$[?(@.orderType == 'CANCEL')].referenceOrderId",
                        everyItem(notNullValue())));
    }

    /**
     * STEP 9 — Verify ticket statuses within the BOOK order in history.
     *
     * The history response embeds the CURRENT state of each ticket.
     * After Step 7's cancellation:
     *   ticket #1 (cancelledTicketId) → CANCELLED
     *   ticket #2                     → ISSUED  (untouched)
     *
     * Assertions are done by parsing the JSON response to avoid brittle
     * JSONPath array-index assumptions.
     */
    @Test
    @Order(9)
    @DisplayName("Step 9 — Cancelled ticket is CANCELLED; other ticket still ISSUED")
    void step9_verifyCancelledTicketStatus() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/bookings/history")
                        .param("userId", USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode history = objectMapper.readTree(result.getResponse().getContentAsString());

        // Locate the BOOK order from the history array
        JsonNode bookOrder = null;
        for (JsonNode order : history) {
            if ("BOOK".equals(order.get("orderType").asText())) {
                bookOrder = order;
                break;
            }
        }
        assertNotNull(bookOrder, "BOOK order must appear in booking history");

        // Walk the tickets embedded in the BOOK order and verify each status
        JsonNode tickets = bookOrder.get("tickets");
        boolean cancelledTicketFound = false;
        boolean issuedTicketFound    = false;

        for (JsonNode ticket : tickets) {
            long   id     = ticket.get("ticketId").asLong();
            String status = ticket.get("status").asText();

            if (id == cancelledTicketId) {
                assertEquals("CANCELLED", status,
                        "Ticket " + id + " was cancelled — status must be CANCELLED");
                cancelledTicketFound = true;
            } else {
                assertEquals("ISSUED", status,
                        "Ticket " + id + " was not cancelled — status must remain ISSUED");
                issuedTicketFound = true;
            }
        }

        assertTrue(cancelledTicketFound,
                "Cancelled ticket (id=" + cancelledTicketId + ") must appear in history");
        assertTrue(issuedTicketFound,
                "The non-cancelled ticket must still appear as ISSUED");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Returns the booking request JSON body.
     * Reused in both the failing attempt (Step 3) and the successful one (Step 5).
     */
    private String buildBookingRequest() {
        return """
                {
                  "userId":          "%s",
                  "eventId":         %d,
                  "seatType":        "%s",
                  "quantity":        %d,
                  "bookingStrategy": "PESSIMISTIC",
                  "pricingStrategy": "FIXED"
                }
                """.formatted(USER_ID, eventId, SEAT_TYPE, QUANTITY);
    }
}
