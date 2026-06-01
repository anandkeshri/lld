Event Ticketing System
Problem Statement
Design and implement a backend restful API service for an event ticketing system that can handle concurrent ticket bookings, and manage inventory. The system should be robust enough to handle multiple simultaneous users trying to book tickets while maintaining data consistency and preventing overselling.

Core Requirements
1. Event Management
   Create events with:
   Event name and description
   Date and time
   Venue and seating capacity
   Multiple ticket types (e.g., VIP, Regular)
   Ticket prices
   Sale start and end dates
   Update event details
   Manage ticket inventory
   View event details and available tickets
2. Ticket Booking System
   Reserve tickets for a limited time period
   Purchase reserved tickets
   Handle concurrent booking requests
   Cancel bookings and process refunds
   View booking history
3. Inventory Management
   Track available tickets in real-time
   Implement locking mechanism for concurrent bookings
   Handle ticket release for failed/expired reservations
   Support different ticket types per event

Technical Requirements
Concurrency Handling
Implement optimistic/pessimistic locking
Handle race conditions
Ensure data consistency
Prevent overselling of tickets
Transaction Management
Implement ACID transactions for bookings
Handle rollbacks for failed operations
Maintain booking history
Track payment status
API Design
Design RESTful APIs
Implement rate limiting
Handle error scenarios