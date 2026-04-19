package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Vehicle extends Item{
    public Vehicle(
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
        return ItemType.VEHICLES;
    }

    public String getInfo(){
        return "VEHICLE" + getSpecialfield();
    }
}
