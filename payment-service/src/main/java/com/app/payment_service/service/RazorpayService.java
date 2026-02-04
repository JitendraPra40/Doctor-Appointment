package com.app.payment_service.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RazorpayService {

    private final RazorpayClient client;

    public RazorpayService(
            @Value("${razorpay.key-id}") String key,
            @Value("${razorpay.key-secret}") String secret
    ) throws RazorpayException {
        this.client = new RazorpayClient(key, secret);
    }

    public Order createOrder(BigDecimal amount) throws RazorpayException {

        JSONObject options = new JSONObject();
        options.put("amount", amount.multiply(BigDecimal.valueOf(100)));
        options.put("currency", "INR");

        return client.orders.create(options);
    }
}
