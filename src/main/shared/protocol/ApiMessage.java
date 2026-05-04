package main.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;

public class ApiMessage {
    private MessageCategory category;
    private MessageType type;
    private String requestId;
    private boolean success;
    private String errorMessage;
    private JsonNode payload;

    public ApiMessage() {
    }

    public ApiMessage(
            MessageCategory category,
            MessageType type,
            String requestId,
            boolean success,
            String errorMessage,
            JsonNode payload
    ) {
        this.category = category;
        this.type = type;
        this.requestId = requestId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.payload = payload;
    }

    public MessageCategory getCategory() {
        return category;
    }

    public void setCategory(MessageCategory category) {
        this.category = category;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
