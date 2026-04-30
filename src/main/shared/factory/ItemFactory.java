package main.shared.factory;

import main.shared.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ItemFactory {
    private ItemFactory(){
    }

    public static Item create(
            ItemType type,
            UUID id,
            LocalDateTime createAt,
            LocalDateTime updateAt,
            UUID sellerId,
            String name,
            String description,
            BigDecimal startingPrice,
            String specialField
    ){
        return switch (type){
            case ELECTRONICS -> new Electronics(id,createAt,updateAt, sellerId, name, description, startingPrice, specialField);

            case ART -> new Art(id,createAt,updateAt, sellerId, name, description, startingPrice, specialField);

            case VEHICLE -> new Vehicle(id,createAt,updateAt, sellerId, name, description, startingPrice, specialField);
        };
    }
}
