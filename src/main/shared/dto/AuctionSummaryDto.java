package main.shared.dto;

import main.shared.model.AuctionStatus;
import main.shared.model.ItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuctionSummaryDto(
        UUID auctionId,
        String itemName,
        ItemType itemType,
        BigDecimal startingPrice,
        BigDecimal currentPrice,
        AuctionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String sellerName,
        String leadingBidderName
) {
}