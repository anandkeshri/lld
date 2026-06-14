Core Requirements
1. Create Meeting Rooms
   Each room must have:
   roomId -> system generated
   Capacity -> user provided
2. Book a Room
   Implement:
   bookRoom(startTime (LocalDatetime), endTime, requiredCapacity) → bookingId or error
   A booking should succeed only if:
   A room exists with capacity ≥ requiredCapacity, and
   The selected room has NO overlapping bookings for the given time interval.
3. Prevent Overlapping Bookings
   Two bookings overlap if:
   startA < endB AND startB < endA
   Intervals are half-open: [start, end)
   (Meaning back-to-back bookings like [10,20) and [20,30) are allowed.)
4. In-Memory Storage
   All data must be stored in-memory using appropriate data structures.
   (Example: List, Map, Sorted List, TreeMap, etc.)

