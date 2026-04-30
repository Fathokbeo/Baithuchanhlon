package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class BidTransaction extends Entity {
    private final UUID auctionId;
    private final UUID bidderId;
    private final String bidderName;
    private final BigDecimal amount;
    private final LocalDateTime bidTime;
    private final BidSource source;

    public BidTransaction(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            UUID auctionId,
            UUID bidderId,
            String bidderName,
            BigDecimal amount,
            LocalDateTime bidTime,
            BidSource source
    ) {
        super(id, createdAt, updatedAt);
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId");
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId");
        this.bidderName = Objects.requireNonNull(bidderName, "bidderName");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.bidTime = Objects.requireNonNull(bidTime, "bidTime");
        this.source = Objects.requireNonNull(source, "source");
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public BidSource getSource() {
        return source;
    }
}
