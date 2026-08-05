package ru.uoles.kafka.sender.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.uoles.kafka.sender.config.SecurityProperties;
import ru.uoles.kafka.sender.model.AuthResponse;
import ru.uoles.kafka.sender.model.LoginRequest;
import ru.uoles.kafka.sender.model.RegisterRequest;
import ru.uoles.kafka.sender.security.model.UserAccount;
import ru.uoles.kafka.sender.security.repository.UserRepository;

import java.util.Locale;

/** Реализует регистрацию пользователей и выдачу JWT после проверки пароля. */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final SecurityProperties properties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!properties.registration().enabled()) {
            throw new RegistrationDisabledException();
        }
        String username = normalize(request.username());
        UserAccount account = userRepository.create(username, passwordEncoder.encode(request.password()),
                request.displayName(), "USER");
        return response(account);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String username = normalize(request.username());
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, request.password()));
        UserAccount account = userRepository.findByUsername(authentication.getName())
                .orElseThrow(InvalidCredentialsException::new);
        return response(account);
    }

    private AuthResponse response(UserAccount account) {
        return new AuthResponse(jwtTokenService.createToken(account), "Bearer", jwtTokenService.expiresInSeconds(),
                new AuthResponse.UserResponse(account.id(), account.username(), account.roles()));
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() { super("Invalid credentials"); }
    }

    public static class RegistrationDisabledException extends RuntimeException {
        public RegistrationDisabledException() { super("Registration is disabled"); }
    }
}
