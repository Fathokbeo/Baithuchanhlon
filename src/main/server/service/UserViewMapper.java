package main.server.service;
import main.shared.dto.SessionUserDto;
import main.shared.dto.UserRowDto;
import main.shared.model.User;

public final class UserViewMapper {
    private UserViewMapper() {
    }

    public static SessionUserDto toSessionUser(User user) {
        return new SessionUserDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    public static UserRowDto toRow(User user) {
        return new UserRowDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getCreateAt());
    }
}
