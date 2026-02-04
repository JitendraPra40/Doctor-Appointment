package com.app.auth_service.service;

import com.app.auth_service.propertiesConfig.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsService {

    private final TwilioProperties twilioProperties;

    public TwilioSmsService(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
    }

    // ✅ REQUIRED
    @PostConstruct
    public void initTwilio() {
        Twilio.init(
                twilioProperties.getAccountSid(),
                twilioProperties.getAuthToken()
        );
    }

    public void sendOtp(String mobile, String otp) {
        try {
            Message.creator(
                    new PhoneNumber(mobile),
                    new PhoneNumber(twilioProperties.getPhoneNumber()),
                    "Your OTP is: " + otp
            ).create();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send OTP", ex);
        }
    }
}
