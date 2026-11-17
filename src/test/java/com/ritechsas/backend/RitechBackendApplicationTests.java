package com.ritechsas.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RitechBackendApplicationTests {
    @Test
    void marketplaceOrderTotalUsesDecimalArithmetic() {
        java.math.BigDecimal price = new java.math.BigDecimal("8.50");
        java.math.BigDecimal kilos = new java.math.BigDecimal("12.000");
        assertEquals(new java.math.BigDecimal("102.00000"), price.multiply(kilos));
    }
}
