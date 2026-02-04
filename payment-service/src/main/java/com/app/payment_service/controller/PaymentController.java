package com.app.payment_service.controller;

import com.app.payment_service.dto.PaymentOrderRequest;
import com.app.payment_service.dto.PaymentOrderResponse;
import com.app.payment_service.service.RazorpayService;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('PATIENT')")
public class PaymentController {

    private final RazorpayService razorpay;

    public PaymentController(RazorpayService razorpay) {
        this.razorpay = razorpay;
    }

    @PostMapping("/order")
    public PaymentOrderResponse createOrder(
            @RequestBody PaymentOrderRequest req)
            throws RazorpayException {

        Order order = razorpay.createOrder(req.amount());

        return new PaymentOrderResponse(
                order.get("id"),
                order.get("amount").toString(),
                order.get("currency")
        );
    }
}

