package com.app.auth_service.TwilioConfig;

import com.app.auth_service.propertiesConfig.TwilioProperties;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {
    private final TwilioProperties twilioProperties;

    public TwilioConfig(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
    }

    @PostConstruct
    public void init() {
        Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
    }
}

