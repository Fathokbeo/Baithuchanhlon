package main.server.service;

import main.shared.model.Auction;
import main.shared.model.AuctionStatus;
import main.shared.model.AutoBidConfig;
import main.shared.model.BidSource;
import main.shared.model.BidTransaction;
import main.shared.model.Role;
import main.shared.model.User;
import main.shared.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class AuctionRulesEngine {
    public static final Duration ANTI_SNIPING_TRIGGER = Duration.ofSeconds(15);
    public static final Duration ANTI_SNIPING_EXTENSION = Duration.ofSeconds(30);

    public void placeManualBid(Auction auction, User bidder, BigDecimal amount, LocalDateTime now) {
        validateBidder(auction, bidder);
        auction.refreshLifecycle(now);
        BigDecimal normalizedAmount = MoneyUtils.normalize(amount);
        ensureAuctionRunning(auction, now);
        if (normalizedAmount.compareTo(auction.getCurrentprice()) <= 0) {
            throw new IllegalArgumentException("Gia dau phai cao hon gia hien tai");
        }
        auction.addBid(createBid(auction, bidder, normalizedAmount, now, BidSource.MANUAL), now);
        applyAntiSniping(auction, now);
        resolveAutoBidCompetition(auction, now);
    }

    public void configureAutoBid(Auction auction, User bidder, BigDecimal maxBid, BigDecimal increment, LocalDateTime now) {
        validateBidder(auction, bidder);
        auction.refreshLifecycle(now);
        if (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.PAID
                || auction.getStatus() == AuctionStatus.CANCELED) {
            throw new IllegalStateException("Phien dau gia da dong");
        }
        BigDecimal normalizedMax = MoneyUtils.normalize(maxBid);
        BigDecimal normalizedIncrement = MoneyUtils.normalize(increment);
        if (normalizedIncrement.signum() <= 0) {
            throw new IllegalArgumentException("Buoc gia phai lon hon 0");
        }
        if (normalizedMax.compareTo(auction.getCurrentprice()) <= 0) {
            throw new IllegalArgumentException("maxBid phai cao hon gia hien tai");
        }
        AutoBidConfig config = auction.findAutoBid(bidder.getId())
                .map(existing -> {
                    existing.update(normalizedMax, normalizedIncrement, now);
                    return existing;
                })
                .orElseGet(() -> new AutoBidConfig(
                        UUID.randomUUID(),
                        now,
                        now,
                        auction.getId(),
                        bidder.getId(),
                        bidder.getDisplayName(),
                        normalizedMax,
                        normalizedIncrement,
                        now,
                        true
                ));
        auction.addOrReplaceAutoBid(config, now);
        if (auction.getStatus() == AuctionStatus.RUNNING
                && auction.getLeadingBidderId() != null
                && !bidder.getId().equals(auction.getLeadingBidderId())) {
            applyAntiSniping(auction, now);
            resolveAutoBidCompetition(auction, now);
        }
    }

    public boolean refreshLifecycle(Auction auction, LocalDateTime now) {
        return auction.refreshLifecycle(now);
    }

    private void validateBidder(Auction auction, User bidder) {
        if (bidder.getRole() != Role.BIDDER) {
            throw new IllegalStateException("Chi bidder moi co the dat gia");
        }
        if (auction.getSellerid().equals(bidder.getId())) {
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

    private void resolveAutoBidCompetition(Auction auction, LocalDateTime now) {
        boolean changed;
        do {
            changed = false;
            List<AutoBidConfig> orderedConfigs = auction.getAutoBidConfigs().stream()
                    .filter(AutoBidConfig::isActive)
                    .filter(config -> !config.getBidderId().equals(auction.getLeadingBidderId()))
                    .sorted(Comparator.comparing(AutoBidConfig::getRegisteredAt))
                    .toList();
            for (AutoBidConfig config : orderedConfigs) {
                BigDecimal nextBid = auction.getCurrentprice().add(config.getIncrement());
                BigDecimal actualBid = config.getMaxBid().min(nextBid);
                if (actualBid.compareTo(auction.getCurrentprice()) > 0) {
                    User systemProxy = new ProxyBidder(config.getBidderId(), config.getBidderName());
                    auction.addBid(createBid(auction, systemProxy, actualBid, now, BidSource.AUTO), now);
                    changed = true;
                    continue;
                }
                AutoBidConfig currentLeaderConfig = auction.getLeadingBidderId() == null
                        ? null
                        : auction.findAutoBid(auction.getLeadingBidderId()).orElse(null);
                if (currentLeaderConfig != null
                        && config.getMaxBid().compareTo(auction.getCurrentprice()) == 0
                        && currentLeaderConfig.getMaxBid().compareTo(auction.getCurrentprice()) == 0
                        && config.getRegisteredAt().isBefore(currentLeaderConfig.getRegisteredAt())) {
                    auction.setLeader(config.getBidderId(), config.getBidderName(), auction.getCurrentprice(), now);
                    changed = true;
                }
            }
        } while (changed);
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

    private static final class ProxyBidder extends User {
        private ProxyBidder(UUID id, String displayName) {
            super(id, LocalDateTime.now(), LocalDateTime.now(), "proxy-" + id, "n/a", displayName, Role.BIDDER);
        }
    }
}