package org.example.slice.eventTicketBookingSystem.payment;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class MockPaymentProcessor {

    public boolean processPayment(Order order) {
        log.info("MOCK_PAYMENT | PROCESSING | orderId={} userId={} amount={}",
                order.getId(), order.getUserId(), order.getTotalAmount());
        // Always succeeds — replace with real gateway integration
        log.info("MOCK_PAYMENT | SUCCESS | orderId={}", order.getId());
        return true;
    }

    public void initiateRefund(Order order, BigDecimal amount) {
        log.info("MOCK_REFUND | INITIATED | orderId={} userId={} refundAmount={}",
                order.getId(), order.getUserId(), amount);
        log.info("MOCK_REFUND | COMPLETED | orderId={}", order.getId());
    }
}
