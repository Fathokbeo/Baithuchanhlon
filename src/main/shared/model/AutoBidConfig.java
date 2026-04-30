package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class AutoBidConfig extends Entity {
    private final UUID auctionId;
    private final UUID bidderId;
    private final String bidderName;
    private BigDecimal maxBid;
    private BigDecimal increment;
    private final LocalDateTime registeredAt;
    private boolean active;

    public AutoBidConfig(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            UUID auctionId,
            UUID bidderId,
            String bidderName,
            BigDecimal maxBid,
            BigDecimal increment,
            LocalDateTime registeredAt,
            boolean active
    ) {
        super(id, createdAt, updatedAt);
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId");
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId");
        this.bidderName = Objects.requireNonNull(bidderName, "bidderName");
        this.maxBid = Objects.requireNonNull(maxBid, "maxBid");
        this.increment = Objects.requireNonNull(increment, "increment");
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
        this.active = active;
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

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public BigDecimal getIncrement() {
        return increment;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public boolean isActive() {
        return active;
    }

    public void update(BigDecimal maxBid, BigDecimal increment, LocalDateTime timestamp) {
        this.maxBid = Objects.requireNonNull(maxBid, "maxBid");
        this.increment = Objects.requireNonNull(increment, "increment");
        this.active = true;
        touch(timestamp);
    }
}
