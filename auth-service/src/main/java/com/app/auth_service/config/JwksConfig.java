package com.app.auth_service.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class JwksConfig {

    @Bean
    public JWKSet jwkSet(RSAPublicKey publicKey) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .keyID("my-key-id") // Same ID as above
                .build();
        return new JWKSet(rsaKey);
    }
}
