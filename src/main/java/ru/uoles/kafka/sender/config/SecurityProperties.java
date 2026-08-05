package ru.uoles.kafka.sender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Настройки локальной JWT-аутентификации и политики безопасности. */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(Jwt jwt, Registration registration, Cors cors, Broker broker) {
    public record Jwt(String issuer, String audience, Duration accessTokenTtl, String privateKey, String publicKey) {
    }
    public record Registration(boolean enabled) {
    }
    public record Cors(List<String> allowedOrigins) {
    }
    public record Broker(List<String> allowedAddresses) {
    }
}
