package main.server.service;

import main.server.dao.AuctionDao;
import main.server.dao.UserDao;
import main.shared.factory.ItemFactory;
import main.shared.model.Auction;
import main.shared.model.AuctionStatus;
import main.shared.model.Item;
import main.shared.model.Role;
import main.shared.model.User;
import main.shared.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AuctionService {
    private final AuctionDao auctionDao;
    private final UserDao userDao;
    private final AuctionRulesEngine rulesEngine;
    private final Map<UUID, Auction> auctions = new ConcurrentHashMap<>();
    private final Map<UUID, Lock> locks = new ConcurrentHashMap<>();

    public AuctionService(AuctionDao auctionDao, UserDao userDao, AuctionRulesEngine rulesEngine) {
        this.auctionDao = auctionDao;
        this.userDao = userDao;
        this.rulesEngine = rulesEngine;
        auctionDao.findAll().forEach(auction -> {
            auctions.put(auction.getId(), auction);
            locks.put(auction.getId(), new ReentrantLock());
        });
    }

    public List<Auction> listAuctions() {
        return auctions.values().stream()
                .sorted(Comparator.comparing(Auction::getStartTime).reversed())
                .toList();
    }

    public List<Auction> listMyAuctions(User seller) {
        ensureSeller(seller);
        return auctions.values().stream()
                .filter(auction -> auction.getSellerId().equals(seller.getId()))
                .sorted(Comparator.comparing(Auction::getStartTime).reversed())
                .toList();
    }

    public Auction getAuction(UUID auctionId) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Khong tim thay phien dau gia");
        }
        return auction;
    }

    public Auction saveAuction(User seller, main.shared.dto.UpsertAuctionRequest request, LocalDateTime now) {
        ensureSeller(seller);
        validateAuctionPayload(request.name(), request.description(), request.startingPrice(), request.startTime(), request.endTime(),
                request.specialField());
        if (request.auctionId() == null) {
            UUID auctionId = UUID.randomUUID();
            Item item = ItemFactory.create(
                    request.itemType(),
                    UUID.randomUUID(),
                    now,
                    now,
                    seller.getId(),
                    request.name(),
                    request.description(),
                    MoneyUtils.normalize(request.startingPrice()),
                    request.specialField()
            );
            Auction auction = new Auction(
                    auctionId,
                    now,
                    now,
                    item,
                    seller.getId(),
                    MoneyUtils.normalize(request.startingPrice()),
                    null,
                    null,
                    null,
                    null,
                    now.isBefore(request.startTime()) ? AuctionStatus.OPEN : AuctionStatus.RUNNING,
                    request.startTime(),
                    request.endTime(),
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            auction.refreshLifecycle(now);
            auctions.put(auctionId, auction);
            locks.put(auctionId, new ReentrantLock());
            auctionDao.saveSnapshot(auction);
            return auction;
        }
        Auction auction = getAuction(request.auctionId());
        if (!auction.getSellerId().equals(seller.getId())) {
            throw new IllegalStateException("Ban khong co quyen sua phien dau gia nay");
        }
        if (!auction.canEdit()) {
            throw new IllegalStateException("Khong the sua phien da co bid hoac da ket thuc");
        }
        Lock lock = lockOf(auction.getId());
        lock.lock();
        try {
            auction.getItem().setName(request.name(), now);
            auction.getItem().setDescription(request.description(), now);
            auction.getItem().setSpecialField(request.specialField(), now);
            auction.getItem().setStartingPrice(MoneyUtils.normalize(request.startingPrice()), now);
            auction.setStartTime(request.startTime(), now);
            auction.setEndTime(request.endTime(), now);
            auction.setLeader(null, null, MoneyUtils.normalize(request.startingPrice()), now);
            auction.setStatus(now.isBefore(request.startTime()) ? AuctionStatus.OPEN : AuctionStatus.RUNNING, now);
            auction.refreshLifecycle(now);
            auctionDao.saveSnapshot(auction);
            return auction;
        } finally {
            lock.unlock();
        }
    }

    public void deleteAuction(User seller, UUID auctionId) {
        ensureSeller(seller);
        Auction auction = getAuction(auctionId);
        if (!auction.getSellerId().equals(seller.getId())) {
            throw new IllegalStateException("Ban khong co quyen xoa phien dau gia nay");
        }
        if (!auction.canEdit()) {
            throw new IllegalStateException("Chi xoa duoc phien chua co bid");
        }
        auctions.remove(auctionId);
        locks.remove(auctionId);
        auctionDao.deleteAuction(auctionId);
    }

    public Auction placeBid(User bidder, UUID auctionId, BigDecimal amount, LocalDateTime now) {
        Auction auction = getAuction(auctionId);
        Lock lock = lockOf(auctionId);
        lock.lock();
        try {
            rulesEngine.placeManualBid(auction, bidder, amount, now);
            auctionDao.saveSnapshot(auction);
            return auction;
        } finally {
            lock.unlock();
        }
    }

    public Auction configureAutoBid(User bidder, UUID auctionId, BigDecimal maxBid, BigDecimal increment, LocalDateTime now) {
        Auction auction = getAuction(auctionId);
        Lock lock = lockOf(auctionId);
        lock.lock();
        try {
            rulesEngine.configureAutoBid(auction, bidder, maxBid, increment, now);
            auctionDao.saveSnapshot(auction);
            return auction;
        } finally {
            lock.unlock();
        }
    }

    public Auction updateStatus(User actor, UUID auctionId, AuctionStatus status, LocalDateTime now) {
        Auction auction = getAuction(auctionId);
        if (actor.getRole() != Role.ADMIN && !auction.getSellerId().equals(actor.getId())) {
            throw new IllegalStateException("Ban khong co quyen cap nhat trang thai");
        }
        Lock lock = lockOf(auctionId);
        lock.lock();
        try {
            if (status == AuctionStatus.PAID) {
                auction.markPaid(now);
            } else if (status == AuctionStatus.CANCELED) {
                auction.cancel(now);
            } else {
                throw new IllegalArgumentException("Chi ho tro chuyen sang PAID hoac CANCELED");
            }
            auctionDao.saveSnapshot(auction);
            return auction;
        } finally {
            lock.unlock();
        }
    }

    public List<Auction> processLifecycleTick(LocalDateTime now) {
        List<Auction> changed = new ArrayList<>();
        for (Auction auction : auctions.values()) {
            Lock lock = lockOf(auction.getId());
            lock.lock();
            try {
                if (rulesEngine.refreshLifecycle(auction, now)) {
                    auctionDao.saveSnapshot(auction);
                    changed.add(auction);
                }
            } finally {
                lock.unlock();
            }
        }
        return changed;
    }

    public String sellerName(Auction auction) {
        return userDao.findById(auction.getSellerId())
                .map(User::getDisplayName)
                .orElse("Unknown Seller");
    }

    private void ensureSeller(User seller) {
        if (seller.getRole() != Role.SELLER && seller.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Chi seller/admin moi duoc quan ly san pham");
        }
    }

    private void validateAuctionPayload(
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String specialField
    ) {
        if (name == null || name.isBlank()
                || description == null || description.isBlank()
                || specialField == null || specialField.isBlank()) {
            throw new IllegalArgumentException("Thong tin san pham khong duoc de trong");
        }
        if (startingPrice == null || startingPrice.signum() < 0) {
            throw new IllegalArgumentException("Gia khoi diem khong hop le");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thoi gian dau gia khong hop le");
        }
    }

    private Lock lockOf(UUID auctionId) {
        return locks.computeIfAbsent(auctionId, key -> new ReentrantLock());
    }
}
