package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class Item extends Entity {
    private final UUID sellerid;
    private String name;
    private String description;
    private BigDecimal startingprice;
    private String specialfield;

    public Item(
            UUID id,
            LocalDateTime createAt,
            LocalDateTime updateAt,
            UUID sellerid,
            String name,
            String description,
            BigDecimal startingprice,
            String specialfield
    ){
        super(id, createAt, updateAt);
        this.sellerid = Objects.requireNonNull(sellerid,"sellerid");
        this.name = User.requireText(name,"name");
        this.description = User.requireText(description,"description");
        this.startingprice = Objects.requireNonNull(startingprice,"startingprice");
        this.specialfield = User.requireText(specialfield,"specialfield");
    }

    public UUID getSellerid() {
        return sellerid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name, LocalDateTime timestamp) {
        this.name = User.requireText(name,"name");
        touch(timestamp);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description, LocalDateTime timestamp) {
        this.description = User.requireText(description, "description");
        touch(timestamp);
    }

    public BigDecimal getStartingprice() {
        return startingprice;
    }

    public void setStartingprice(BigDecimal startingprice, LocalDateTime timestamp) {
        this.startingprice = Objects.requireNonNull(startingprice,"starting price");
        touch(timestamp);
    }

    public void setSpecialfield(String specialfield,LocalDateTime timestamp) {
        this.specialfield = User.requireText(specialfield,"specialfield");
        touch(timestamp);
    }

    public String getSpecialfield() {
        return specialfield;
    }
}
