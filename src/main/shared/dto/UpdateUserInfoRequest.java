package main.shared.dto;

import java.math.BigDecimal;

public record UpdateUserInfoRequest(String displayName, BigDecimal balance) {
}
