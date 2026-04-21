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
    private final BidSource source;
    private final LocalDateTime bidTime;

    public BidTransaction(
            UUID id,
            LocalDateTime createAt,
            LocalDateTime updateAt,
            UUID auctionId,
            UUID bidderId,
            String bidderName,
            BigDecimal amount,
            BidSource source,
            LocalDateTime bidTime
    ){
        super(id,createAt,updateAt);
        this.auctionId = Objects.requireNonNull(auctionId,"auctionId");
        this.bidderId = Objects.requireNonNull(bidderId,"bidderId");
        this.bidderName = Objects.requireNonNull(bidderName,"bidderName");
        this.amount = Objects.requireNonNull(amount,"amount");
        this.source = Objects.requireNonNull(source,"source");
        this.bidTime = Objects.requireNonNull(bidTime,"bidTime");
    }

    public BidSource getSource() {
        return source;
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
}
