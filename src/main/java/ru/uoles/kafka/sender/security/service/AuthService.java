package ru.uoles.kafka.sender.security.service;

import ru.uoles.kafka.sender.model.AuthResponse;
import ru.uoles.kafka.sender.model.LoginRequest;
import ru.uoles.kafka.sender.model.RegisterRequest;

/** Сервис регистрации и аутентификации пользователей. */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
