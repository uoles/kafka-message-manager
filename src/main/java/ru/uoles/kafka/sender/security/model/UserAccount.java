package ru.uoles.kafka.sender.security.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Сохранённая учётная запись пользователя с ролями. */
public record UserAccount(UUID id, String username, String passwordHash, String displayName,
                          boolean enabled, Instant createdAt, Instant updatedAt, List<String> roles) {
}
