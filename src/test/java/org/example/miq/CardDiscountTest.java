package org.example.miq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDiscountTest {

    CardDiscount cardDiscount;

    @BeforeEach
    void setUp() {
        cardDiscount = new CardDiscount();
    }

    // --- Valid cards ---

    @Test
    void validVisa_returns10PercentDiscount() {
        // 4111111111111111 is a well-known Visa test number that passes Luhn
        double discount = cardDiscount.calculateDiscount(100.0, "4111111111111111");
        assertEquals(10.0, discount);
    }

    @Test
    void validVisa_discountScalesWithBillAmount() {
        double discount = cardDiscount.calculateDiscount(200.0, "4111111111111111");
        assertEquals(20.0, discount);
    }

    @Test
    void validMastercard_returns15PercentDiscount() {
        // 5500005555555559 is a valid Mastercard number (passes Luhn, starts with 5)
        double discount = cardDiscount.calculateDiscount(100.0, "5500005555555559");
        assertEquals(15.0, discount);
    }

    @Test
    void validMastercard_discountScalesWithBillAmount() {
        double discount = cardDiscount.calculateDiscount(200.0, "5500005555555559");
        assertEquals(30.0, discount);
    }

    // --- Invalid cards ---

    @Test
    void invalidCard_tooShort_returnsZero() {
        double discount = cardDiscount.calculateDiscount(100.0, "411111111111111"); // 15 digits
        assertEquals(0.0, discount);
    }

    @Test
    void invalidCard_tooLong_returnsZero() {
        double discount = cardDiscount.calculateDiscount(100.0, "41111111111111112"); // 17 digits
        assertEquals(0.0, discount);
    }

    @Test
    void invalidCard_nonNumericCharacters_returnsZero() {
        double discount = cardDiscount.calculateDiscount(100.0, "411111111111111A"); // 16 chars, non-numeric
        assertEquals(0.0, discount);
    }

    @Test
    void invalidCard_failsLuhnCheck_returnsZero() {
        // 4111111111111112 has correct length and starts with 4 but fails Luhn
        double discount = cardDiscount.calculateDiscount(100.0, "4111111111111112");
        assertEquals(0.0, discount);
    }
}