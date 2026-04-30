package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Auction extends Entity {
    private final Item item;
    private final UUID sellerId;
    private BigDecimal currentPrice;
    private UUID leadingBidderId;
    private String leadingBidderName;
    private UUID winnerBidderId;
    private String winnerBidderName;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int extensionCount;
    private final List<BidTransaction> bidHistory;
    private final List<AutoBidConfig> autoBidConfigs;

    public Auction(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Item item,
            UUID sellerId,
            BigDecimal currentPrice,
            UUID leadingBidderId,
            String leadingBidderName,
            UUID winnerBidderId,
            String winnerBidderName,
            AuctionStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int extensionCount,
            List<BidTransaction> bidHistory,
            List<AutoBidConfig> autoBidConfigs
    ) {
        super(id, createdAt, updatedAt);
        this.item = Objects.requireNonNull(item, "item");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId");
        this.currentPrice = Objects.requireNonNull(currentPrice, "currentPrice");
        this.leadingBidderId = leadingBidderId;
        this.leadingBidderName = leadingBidderName;
        this.winnerBidderId = winnerBidderId;
        this.winnerBidderName = winnerBidderName;
        this.status = Objects.requireNonNull(status, "status");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        this.extensionCount = extensionCount;
        this.bidHistory = new ArrayList<>(Objects.requireNonNull(bidHistory, "bidHistory"));
        this.autoBidConfigs = new ArrayList<>(Objects.requireNonNull(autoBidConfigs, "autoBidConfigs"));
    }

    public Item getItem() {
        return item;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public UUID getLeadingBidderId() {
        return leadingBidderId;
    }

    public String getLeadingBidderName() {
        return leadingBidderName;
    }

    public void setLeader(UUID bidderId, String bidderName, BigDecimal amount, LocalDateTime timestamp) {
        this.leadingBidderId = bidderId;
        this.leadingBidderName = bidderName;
        this.currentPrice = amount;
        touch(timestamp);
    }

    public UUID getWinnerBidderId() {
        return winnerBidderId;
    }

    public String getWinnerBidderName() {
        return winnerBidderName;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status, LocalDateTime timestamp) {
        this.status = Objects.requireNonNull(status, "status");
        touch(timestamp);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime, LocalDateTime timestamp) {
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        touch(timestamp);
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime, LocalDateTime timestamp) {
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        touch(timestamp);
    }

    public int getExtensionCount() {
        return extensionCount;
    }

    public void incrementExtensionCount() {
        extensionCount++;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public List<AutoBidConfig> getAutoBidConfigs() {
        return autoBidConfigs;
    }

    public void addBid(BidTransaction bid, LocalDateTime timestamp) {
        bidHistory.add(Objects.requireNonNull(bid, "bid"));
        setLeader(bid.getBidderId(), bid.getBidderName(), bid.getAmount(), timestamp);
    }

    public Optional<AutoBidConfig> findAutoBid(UUID bidderId) {
        return autoBidConfigs.stream()
                .filter(config -> config.getBidderId().equals(bidderId))
                .findFirst();
    }

    public void addOrReplaceAutoBid(AutoBidConfig config, LocalDateTime timestamp) {
        autoBidConfigs.removeIf(existing -> existing.getBidderId().equals(config.getBidderId()));
        autoBidConfigs.add(config);
        touch(timestamp);
    }

    public boolean canEdit() {
        return bidHistory.isEmpty() && status != AuctionStatus.FINISHED && status != AuctionStatus.PAID;
    }

    public boolean refreshLifecycle(LocalDateTime now) {
        boolean changed = false;
        if (status == AuctionStatus.CANCELED || status == AuctionStatus.PAID) {
            return false;
        }
        if (status == AuctionStatus.OPEN && !now.isBefore(startTime)) {
            status = AuctionStatus.RUNNING;
            changed = true;
        }
        if ((status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) && !now.isBefore(endTime)) {
            status = AuctionStatus.FINISHED;
            winnerBidderId = leadingBidderId;
            winnerBidderName = leadingBidderName;
            changed = true;
        }
        if (changed) {
            touch(now);
        }
        return changed;
    }

    public void markPaid(LocalDateTime timestamp) {
        if (status != AuctionStatus.FINISHED) {
            throw new IllegalStateException("Only finished auctions can be marked as paid");
        }
        status = AuctionStatus.PAID;
        touch(timestamp);
    }

    public void cancel(LocalDateTime timestamp) {
        if (status == AuctionStatus.PAID) {
            throw new IllegalStateException("Paid auctions cannot be canceled");
        }
        status = AuctionStatus.CANCELED;
        touch(timestamp);
    }
}
