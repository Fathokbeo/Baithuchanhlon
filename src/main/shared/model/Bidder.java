package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Bidder extends User {
    public Bidder(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String displayName,
            BigDecimal balance
    ) {
        super(id, createdAt, updatedAt, username, passwordHash, displayName, Role.BIDDER, balance);
    }

    public Bidder(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String displayName,
            BigDecimal balance,
            String email,
            String phone,
            String address
    ) {
        super(id, createdAt, updatedAt, username, passwordHash, displayName, Role.BIDDER, balance, email, phone, address);
    }
}
