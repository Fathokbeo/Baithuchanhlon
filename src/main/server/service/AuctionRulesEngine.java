package main.server.service;

import main.shared.model.Auction;
import main.shared.model.AuctionStatus;
import main.shared.model.BidSource;
import main.shared.model.BidTransaction;
import main.shared.model.Role;
import main.shared.model.User;
import main.shared.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public final class AuctionRulesEngine {
    public static final Duration ANTI_SNIPING_TRIGGER = Duration.ofSeconds(15);
    public static final Duration ANTI_SNIPING_EXTENSION = Duration.ofSeconds(30);

    public void placeManualBid(Auction auction, User bidder, BigDecimal amount, LocalDateTime now) {
        validateBidder(auction, bidder);
        auction.refreshLifecycle(now);
        BigDecimal normalizedAmount = MoneyUtils.normalize(amount);
        ensureAuctionRunning(auction, now);
        if (normalizedAmount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException("Gia dau phai cao hon gia hien tai");
        }
        auction.addBid(createBid(auction, bidder, normalizedAmount, now, BidSource.MANUAL), now);
        applyAntiSniping(auction, now);
    }

    public boolean refreshLifecycle(Auction auction, LocalDateTime now) {
        return auction.refreshLifecycle(now);
    }

    private void validateBidder(Auction auction, User bidder) {
        if (bidder.getRole() != Role.BIDDER) {
            throw new IllegalStateException("Chi bidder moi co the dat gia");
        }
        if (auction.getSellerId().equals(bidder.getId())) {
            throw new IllegalStateException("Seller khong duoc tu dau gia san pham cua minh");
        }
    }

    private void ensureAuctionRunning(Auction auction, LocalDateTime now) {
        if (auction.getStatus() == AuctionStatus.OPEN && now.isBefore(auction.getStartTime())) {
            throw new IllegalStateException("Phien dau gia chua bat dau");
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new IllegalStateException("Phien dau gia da dong");
        }
        if (!now.isBefore(auction.getEndTime())) {
            throw new IllegalStateException("Phien dau gia da het han");
        }
    }

    private void applyAntiSniping(Auction auction, LocalDateTime now) {
        LocalDateTime triggerTime = auction.getEndTime().minus(ANTI_SNIPING_TRIGGER);
        if (!now.isBefore(triggerTime) && now.isBefore(auction.getEndTime())) {
            auction.setEndTime(auction.getEndTime().plus(ANTI_SNIPING_EXTENSION), now);
            auction.incrementExtensionCount();
        }
    }

    private BidTransaction createBid(Auction auction, User bidder, BigDecimal amount, LocalDateTime now, BidSource source) {
        return new BidTransaction(
                UUID.randomUUID(),
                now,
                now,
                auction.getId(),
                bidder.getId(),
                bidder.getDisplayName(),
                amount,
                now,
                source
        );
    }
}
