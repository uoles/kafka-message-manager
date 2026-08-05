package ru.uoles.kafka.sender.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Запрос на регистрацию пользователя. */
public record RegisterRequest(
        @NotBlank(message = "Username is required") @Size(max = 255, message = "Username is too long") String username,
        @NotBlank(message = "Password is required") @Size(min = 12, max = 128, message = "Password must contain 12 to 128 characters") String password,
        @Size(max = 255, message = "Display name is too long") String displayName) {
}
