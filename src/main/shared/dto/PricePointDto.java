package main.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PricePointDto(LocalDateTime timestamp, BigDecimal amount) {
}
