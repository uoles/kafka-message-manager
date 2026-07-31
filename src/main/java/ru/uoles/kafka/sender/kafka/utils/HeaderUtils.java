package ru.uoles.kafka.sender.kafka.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Разбирает строковое представление заголовков Kafka.
 */
public final class HeaderUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Запрещает создание экземпляров служебного класса парсера. */
    private HeaderUtils() {}

    /**
     * Преобразует строку заголовков в список Kafka-заголовков.
     *
     * @param headers строка в формате name=value,name2=value2
     * @return список заголовков с UTF-8 значениями
     * @throws IllegalArgumentException если строка содержит некорректный заголовок
     */
    public static List<Header> parse(String headers) {
        if (headers == null || headers.isBlank()) {
            return List.of();
        }

        List<Header> parsedHeaders = new ArrayList<>();
        Set<String> names = new HashSet<>();
        String[] entries = headers.split(",", -1);
        for (String entry : entries) {
            int separator = entry.indexOf('=');
            if (separator < 0) {
                throw new IllegalArgumentException("Invalid Kafka header: expected name=value");
            }

            String name = entry.substring(0, separator).trim();
            String value = entry.substring(separator + 1).trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Kafka header name must not be empty");
            }
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Kafka header value must not be empty");
            }
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate Kafka header: " + name);
            }
            parsedHeaders.add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
        }
        return List.copyOf(parsedHeaders);
    }

    public static String serializeHeaders(List<ConsumerMessageResponse.ConsumerHeaderResponse> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Kafka headers", exception);
        }
    }

    public static List<ConsumerMessageResponse.ConsumerHeaderResponse> deserializeHeaders(String headers) {
        try {
            return objectMapper.readValue(headers, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize Kafka headers", exception);
        }
    }
}
