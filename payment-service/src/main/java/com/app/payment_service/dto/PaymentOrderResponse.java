package com.app.payment_service.dto;

public record PaymentOrderResponse(
        String orderId,
        String amount,
        String currency
) {}
