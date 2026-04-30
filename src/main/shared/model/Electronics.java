package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Electronics extends Item {
    public Electronics(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            UUID sellerId,
            String name,
            String description,
            BigDecimal startingPrice,
            String specialField
    ) {
        super(id, createdAt, updatedAt, sellerId, name, description, startingPrice, specialField);
    }

    @Override
    public ItemType getType() {
        return ItemType.ELECTRONICS;
    }

    @Override
    public String printInfo() {
        return "Electronics | " + getSpecialField();
    }
}
