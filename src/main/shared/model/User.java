package main.shared.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class User extends Entity {
    private final String username;
    private final String passwordHash;
    private String displayName;
    private final Role role;

    protected User(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String displayName,
            Role role
    ) {
        super(id, createdAt, updatedAt);
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.displayName = requireText(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role");
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName, LocalDateTime timestamp) {
        this.displayName = requireText(displayName, "displayName");
        touch(timestamp);
    }

    public Role getRole() {
        return role;
    }

    protected static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
