package main.shared.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User extends Entity{
    private final String username;
    private final String password;
    private String displayName;
    Role role;

    public User(
            UUID id,
            LocalDateTime createAt,
            LocalDateTime updateAt,
            String username,
            String password,
            String displayName,
            Role role
    ){
        super(id,createAt,updateAt);
        this.username = requireText(username,"username");
        this.password = requireText(password,"password");
        this.displayName = requireText(displayName,"displayName");
        this.role = Objects.requireNonNull(role,"role");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDisplayName(){
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public void setDisplayName(String displayName,LocalDateTime timestamp) {
        this.displayName = requireText(displayName,"displayName");
        touch(timestamp);
    }

    protected static String requireText(String value, String fieldName){
        if(value == null || value.isBlank()){
            throw new IllegalAccessException(fieldName + "must not be blank");
        }
        return value.trim();
    }

}
