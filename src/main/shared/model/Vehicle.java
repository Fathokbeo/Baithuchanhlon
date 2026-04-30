package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Vehicle extends Item {
    public Vehicle(
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
        return ItemType.VEHICLE;
    }

    @Override
    public String printInfo() {
        return "Vehicle | " + getSpecialField();
    }
}
