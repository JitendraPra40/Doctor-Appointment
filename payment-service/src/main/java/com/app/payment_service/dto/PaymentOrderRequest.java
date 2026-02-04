package com.app.payment_service.dto;

import java.math.BigDecimal;

public record PaymentOrderRequest(
        BigDecimal amount
) {


}

