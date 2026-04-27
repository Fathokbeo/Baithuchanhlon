package main.shared.dto;

import main.shared.model.Role;

public record RegisterRequest(String username, String password, String displayName, Role role) {
}
