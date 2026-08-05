package ru.uoles.kafka.sender.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.uoles.kafka.sender.kafka.consumer.ConsumerManager;
import ru.uoles.kafka.sender.kafka.service.KafkaMessageService;
import ru.uoles.kafka.sender.model.*;

import java.util.List;
import java.util.UUID;

/**
 * REST-контроллер отправки сообщений в Kafka и проверки состояния сервиса.
 */
@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class MessageController {

    /** Сервис отправки сообщений в Kafka. */
    private final KafkaMessageService kafkaMessageService;

    /** Менеджер жизненного цикла динамических Kafka-потребителей. */
    private final ConsumerManager consumerManager;

    /**
     * Отправляет сообщение в Kafka на основании тела запроса.
     *
     * @param request валидированный запрос с параметрами сообщения
     * @return HTTP-ответ с результатом отправки
     */
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
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
     * Создаёт и запускает динамического Kafka-потребителя.
     *
     * @param request валидированный запрос с адресом брокера и названием топика
     * @return HTTP-ответ с данными созданного потребителя
     */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PostMapping("/consumers")
    public ResponseEntity<ConsumerResponse> createConsumer(@Valid @RequestBody CreateConsumerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consumerManager.create(request.getBootstrapAddress(), request.getTopic()));
    }

    /**
     * Возвращает список всех динамических Kafka-потребителей.
     *
     * @return HTTP-ответ со списком потребителей
     */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/consumers")
    public ResponseEntity<List<ConsumerResponse>> listConsumers() {
        return ResponseEntity.ok(consumerManager.list());
    }

    /**
     * Возвращает сведения о динамическом Kafka-потребителе.
     *
     * @param id уникальный идентификатор потребителя
     * @return HTTP-ответ с данными потребителя
     */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/consumers/{id}")
    public ResponseEntity<ConsumerResponse> getConsumer(@PathVariable UUID id) {
        return ResponseEntity.ok(consumerManager.get(id));
    }

    /**
     * Возвращает порцию сообщений из буфера потребителя после указанного курсора.
     *
     * @param id уникальный идентификатор потребителя
     * @param after эксклюзивный курсор последнего обработанного сообщения
     * @param limit максимальное количество возвращаемых сообщений
     * @return HTTP-ответ с сообщениями и метаданными курсора
     */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/consumers/{id}/messages")
    public ResponseEntity<ConsumerMessagesResponse> getConsumerMessages(@PathVariable UUID id,
                                                                          @RequestParam(defaultValue = "0") long after,
                                                                          @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(consumerManager.messages(id, after, limit));
    }

    /**
     * Останавливает и удаляет динамического Kafka-потребителя.
     *
     * @param id уникальный идентификатор удаляемого потребителя
     * @return пустой HTTP-ответ со статусом успешного удаления
     */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @DeleteMapping("/consumers/{id}")
    public ResponseEntity<Void> deleteConsumer(@PathVariable UUID id) {
        consumerManager.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует ошибку отсутствующего потребителя в HTTP-ответ.
     *
     * @param exception исключение с описанием отсутствующего потребителя
     * @return HTTP-ответ со статусом 404 и описанием ошибки
     */
    @ExceptionHandler(ConsumerManager.ConsumerNotFoundException.class)
    public ResponseEntity<MessageResponse> handleConsumerNotFound(ConsumerManager.ConsumerNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("error", exception.getMessage(), null, null, System.currentTimeMillis()));
    }

    /**
     * Преобразует превышение лимита потребителей в HTTP-ответ.
     *
     * @param exception исключение о достижении максимального количества потребителей
     * @return HTTP-ответ со статусом 409 и описанием ошибки
     */
    @ExceptionHandler(ConsumerManager.ConsumerLimitException.class)
    public ResponseEntity<MessageResponse> handleConsumerLimit(ConsumerManager.ConsumerLimitException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse("error", exception.getMessage(), null, null, System.currentTimeMillis()));
    }

    /**
     * Проверяет доступность сервиса отправки сообщений в Kafka.
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
