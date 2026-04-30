package main.shared.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Bidder extends User {
    public Bidder(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String displayName
    ) {
        super(id, createdAt, updatedAt, username, passwordHash, displayName, Role.BIDDER);
    }
}
