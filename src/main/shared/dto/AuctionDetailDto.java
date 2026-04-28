package main.shared.dto;

import main.shared.model.AuctionStatus;
import main.shared.model.ItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public record AuctionDetailDto(
        UUID auctionId,
        UUID sellerId,
        String sellerName,
        String itemName,
        String description,
        String itemInfo,
        ItemType itemType,
        BigDecimal startingPrice,
        BigDecimal currentPrice,
        AuctionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String leadingBidderName,
        String winnerBidderName,
        int extensionCount,
        List<BidTransactionDto> bidHistory,
        List<PricePointDto> priceHistory,
        List<AutoBidDto> autoBids
) {
}
