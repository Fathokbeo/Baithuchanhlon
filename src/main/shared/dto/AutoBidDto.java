package main.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AutoBidDto(String bidderName, BigDecimal maxBid, BigDecimal increment, LocalDateTime registeredAt) {
}
