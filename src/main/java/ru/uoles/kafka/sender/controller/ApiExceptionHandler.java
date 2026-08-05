package ru.uoles.kafka.sender.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.uoles.kafka.sender.model.MessageResponse;
import ru.uoles.kafka.sender.security.repository.UserRepository;
import ru.uoles.kafka.sender.security.service.AuthServiceImpl;

/** Единообразно скрывает внутренние детали ошибок API. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<MessageResponse> badRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(UserRepository.DuplicateUsernameException.class)
    public ResponseEntity<MessageResponse> duplicateUsername(UserRepository.DuplicateUsernameException exception) {
        return response(HttpStatus.CONFLICT, "Username is already registered");
    }

    @ExceptionHandler({AuthenticationException.class, AuthServiceImpl.InvalidCredentialsException.class})
    public ResponseEntity<MessageResponse> invalidCredentials(Exception exception) {
        return response(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(AuthServiceImpl.RegistrationDisabledException.class)
    public ResponseEntity<MessageResponse> registrationDisabled() {
        return response(HttpStatus.FORBIDDEN, "Registration is disabled");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> unexpected(Exception exception, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "Request could not be processed");
        }
        throw new RuntimeException(exception);
    }

    private ResponseEntity<MessageResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new MessageResponse("error", message, null, null, System.currentTimeMillis()));
    }
}
