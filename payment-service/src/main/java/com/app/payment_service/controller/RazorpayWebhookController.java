package com.app.payment_service.controller;

import com.app.payment_service.service.PaymentService;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/webhook")
public class RazorpayWebhookController {

    private final PaymentService service;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public RazorpayWebhookController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String payload) throws Exception {

        Utils.verifyWebhookSignature(payload, signature, webhookSecret);

        JSONObject json = new JSONObject(payload);
        JSONObject entity = json.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        service.confirmPayment(
                entity.getString("order_id"),
                entity.getString("id"),
                entity.getBigDecimal("amount").divide(BigDecimal.valueOf(100)),
                UUID.fromString(entity.getJSONObject("notes")
                        .getString("appointmentId"))
        );

        return ResponseEntity.ok().build();
    }
}

