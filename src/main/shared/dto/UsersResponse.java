package main.shared.dto;

import java.util.List;

public record UsersResponse(List<UserRowDto> users) {
}
