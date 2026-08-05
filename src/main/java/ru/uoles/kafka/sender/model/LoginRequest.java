package ru.uoles.kafka.sender.model;

import jakarta.validation.constraints.NotBlank;

/** Запрос на вход пользователя. */
public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password) {
}
