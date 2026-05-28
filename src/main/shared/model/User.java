package main.shared.model;

import main.shared.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class User extends Entity {
    private final String username;
    private final String passwordHash;
    private String displayName;
    private final Role role;
    BigDecimal balance;
    private String email;
    private String phone;
    private String address;

    protected User(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String displayName,
            Role role,
            BigDecimal balance
    ) {
        this(id, createdAt, updatedAt, username, passwordHash, displayName, role, balance, "", "", "");
    }

    protected User(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String displayName,
            Role role,
            BigDecimal balance,
            String email,
            String phone,
            String address
    ) {
        super(id, createdAt, updatedAt);
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.displayName = requireText(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role");
        this.balance = MoneyUtils.normalize(Objects.requireNonNull(balance, "balance"));
        this.email = normalizeOptional(email);
        this.phone = normalizeOptional(phone);
        this.address = normalizeOptional(address);
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
    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance, LocalDateTime timestamp) {
        this.balance = MoneyUtils.normalize(Objects.requireNonNull(balance, "balance"));
        touch(timestamp);
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public void setContactInfo(String email, String phone, String address, LocalDateTime timestamp) {
        this.email = normalizeOptional(email);
        this.phone = normalizeOptional(phone);
        this.address = normalizeOptional(address);
        touch(timestamp);
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
