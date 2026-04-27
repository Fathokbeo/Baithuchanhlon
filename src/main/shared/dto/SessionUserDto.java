package main.shared.dto;

import main.shared.model.Role;

import java.util.UUID;

public record SessionUserDto(UUID id, String username, String displayName, Role role) {
}
