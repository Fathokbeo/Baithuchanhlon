package main.shared.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Bidder extends User {
    public Bidder(
            UUID id,
            LocalDateTime createAt,
            LocalDateTime updateAt,
            String username,
            String password,
            String displayName
    ){
        super(id,createAt,updateAt,username,password,displayName,Role.BIDDER);
    }
}
