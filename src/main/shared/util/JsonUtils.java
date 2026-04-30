package main.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static JsonNode toJsonNode(Object value) {
        return MAPPER.valueToTree(value);
    }

    public static <T> T fromJsonNode(JsonNode node, Class<T> targetType) {
        return MAPPER.convertValue(node, targetType);
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize object", exception);
        }
    }

    public static <T> T read(String json, Class<T> targetType) {
        try {
            return MAPPER.readValue(json, targetType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize json", exception);
        }
    }
}
