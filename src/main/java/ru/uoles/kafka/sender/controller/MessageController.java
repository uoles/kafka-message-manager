package ru.uoles.kafka.sender.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.uoles.kafka.sender.model.MessageRequest;
import ru.uoles.kafka.sender.model.MessageResponse;
import ru.uoles.kafka.sender.service.KafkaMessageService;

/**
 * REST-контроллер отправки сообщений в Kafka и проверки состояния сервиса.
 */
@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class MessageController {

    private final KafkaMessageService kafkaMessageService;

    /**
     * Отправляет сообщение в Kafka на основании тела запроса.
     *
     * @param request валидированный запрос с параметрами сообщения
     * @return HTTP-ответ с результатом отправки
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        log.info("Received request to send message to topic: {}, kafka: {}",
                request.getTopic(), request.getKafkaAddress());

        try {
            kafkaMessageService.sendMessage(
                    request.getTopic(),
                    request.getKafkaAddress(),
                    request.getMessageText(),
                    request.getHeaders()
            );

            MessageResponse response = new MessageResponse(
                    "success",
                    "Message sent successfully to topic: " + request.getTopic(),
                    request.getTopic(),
                    request.getKafkaAddress(),
                    System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to send message", e);
            MessageResponse errorResponse = new MessageResponse(
                    "error",
                    "Failed to send message: " + e.getMessage(),
                    request.getTopic(),
                    request.getKafkaAddress(),
                    System.currentTimeMillis()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Преобразует ошибку формата Kafka-заголовков в HTTP-ответ с кодом 400.
     *
     * @param exception исключение с описанием некорректных заголовков
     * @return HTTP-ответ с описанием ошибки клиента
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleInvalidHeaders(IllegalArgumentException exception) {
        MessageResponse response = new MessageResponse(
                "error",
                exception.getMessage(),
                null,
                null,
                System.currentTimeMillis()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Проверяет доступность Kafka-сервиса.
     *
     * @return успешный HTTP-ответ со статусом сервиса
     */
    @GetMapping("/health")
    public ResponseEntity<MessageResponse> healthCheck() {
        MessageResponse response = new MessageResponse(
                "success",
                "Kafka Producer Service is running",
                null,
                null,
                System.currentTimeMillis()
        );
        return ResponseEntity.ok(response);
    }
}
