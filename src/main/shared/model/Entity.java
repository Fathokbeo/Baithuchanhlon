package main.shared.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class Entity {
    private final UUID id;
    private final LocalDateTime createAt;
    private LocalDateTime updateAt;

    public Entity(UUID id, LocalDateTime createAt, LocalDateTime updateAt){
        this.id = Objects.requireNonNull(id,"id");
        this.createAt = Objects.requireNonNull(createAt,"createAt");
        this.updateAt = Objects.requireNonNull(updateAt,"updateAt");
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void touch(LocalDateTime timestamp){
        updateAt = Objects.requireNonNull(timestamp,"timestamp");
    }
}
