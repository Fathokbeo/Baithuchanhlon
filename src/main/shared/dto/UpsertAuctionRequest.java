package main.shared.dto;

import main.shared.model.ItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpsertAuctionRequest(
        UUID auctionId,
        ItemType itemType,
        String name,
        String description,
        BigDecimal startingPrice,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String specialField
) {
}
