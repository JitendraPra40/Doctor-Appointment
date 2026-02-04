package com.app.auth_service.propertiesConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private Access access = new Access();
    private Refresh refresh = new Refresh();

    public static class Access {
        private long expiration;
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
    }

    public static class Refresh {
        private long expiration;
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
    }

    public Access getAccess() { return access; }
    public Refresh getRefresh() { return refresh; }
}


