package main.shared.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfigureAutoBidRequest(UUID auctionId, BigDecimal maxBid, BigDecimal increment) {
}
