package main.shared.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceBidRequest(UUID auctionId, BigDecimal amount) {
}
