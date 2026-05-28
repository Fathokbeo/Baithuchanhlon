package main.shared.dto;

import java.math.BigDecimal;

public record UpdateUserInfoRequest(String displayName, BigDecimal balance, String email, String phone, String address) {
}
