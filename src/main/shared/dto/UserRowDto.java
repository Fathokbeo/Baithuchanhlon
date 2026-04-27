package main.shared.dto;

import main.shared.model.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRowDto(UUID id, String username, String displayName, Role role, LocalDateTime createdAt) {
}

