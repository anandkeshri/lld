package org.example.slice.eventTicketBookingSystem.model;

public enum OrderStatus {
    CREATED,
    SEATS_RESERVED,
    PAYMENT_PROCESSING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    SEATS_UNAVAILABLE,
    TICKETS_ISSUED,
    TICKET_ISSUE_FAILED,
    REFUND_INITIATED,
    REFUND_COMPLETED,
    CANCELLED
}