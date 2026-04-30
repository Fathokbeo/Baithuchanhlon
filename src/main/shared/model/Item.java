package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class Item extends Entity {
    private final UUID sellerId;
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private String specialField;

    protected Item(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            UUID sellerId,
            String name,
            String description,
            BigDecimal startingPrice,
            String specialField
    ) {
        super(id, createdAt, updatedAt);
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId");
        this.name = User.requireText(name, "name");
        this.description = User.requireText(description, "description");
        this.startingPrice = Objects.requireNonNull(startingPrice, "startingPrice");
        this.specialField = User.requireText(specialField, "specialField");
    }

    public abstract ItemType getType();

    public abstract String printInfo();

    public UUID getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name, LocalDateTime timestamp) {
        this.name = User.requireText(name, "name");
        touch(timestamp);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description, LocalDateTime timestamp) {
        this.description = User.requireText(description, "description");
        touch(timestamp);
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice, LocalDateTime timestamp) {
        this.startingPrice = Objects.requireNonNull(startingPrice, "startingPrice");
        touch(timestamp);
    }

    public String getSpecialField() {
        return specialField;
    }

    public void setSpecialField(String specialField, LocalDateTime timestamp) {
        this.specialField = User.requireText(specialField, "specialField");
        touch(timestamp);
    }
}
