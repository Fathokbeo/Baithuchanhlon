package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Electronic extends Item {
    public Electronic(
            UUID id,
            LocalDateTime createAt,
            LocalDateTime updateAt,
            UUID sellerid,
            String name,
            String description,
            BigDecimal startingprice,
            String specialfield
    ){
        super(id,createAt,updateAt,sellerid,name,description,startingprice,specialfield);
    }

    public ItemType getType(){
        return ItemType.ELECTRONICS;
    }

    public String getInfo(){
        return "ELECTRONIC "+ getSpecialfield();
    }
}
