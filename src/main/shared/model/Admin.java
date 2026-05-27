package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Admin extends User {
    public Admin(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String displayName,
            BigDecimal balance
    ) {
        super(id, createdAt, updatedAt, username, passwordHash, displayName, Role.ADMIN, balance);
    }
}
