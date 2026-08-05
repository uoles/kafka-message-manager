package ru.uoles.kafka.sender.model;

import java.util.List;
import java.util.UUID;

/** Безопасный ответ с токеном доступа и сведениями о пользователе. */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    /** Открытые сведения о пользователе. */
    public record UserResponse(UUID id, String username, List<String> roles) {
    }
}
