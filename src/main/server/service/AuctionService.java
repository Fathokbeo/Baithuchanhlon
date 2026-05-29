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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AuctionService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

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
        reconcileWalletStateOnStartup(LocalDateTime.now());
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
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
        }
        return auction;
    }

    public Auction saveAuction(User seller, main.shared.dto.UpsertAuctionRequest request, LocalDateTime now) {
        ensureSeller(seller);
        validateAuctionPayload(request.name(), request.description(), request.startingPrice(), request.startTime(), request.endTime(),
                request.specialField());
        BigDecimal minRaise = request.minRaise() == null ? BigDecimal.ZERO : MoneyUtils.normalize(request.minRaise());
        if (minRaise.signum() < 0) {
            throw new IllegalArgumentException("MinRaise không được âm");
        }
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
                    request.imagePath(),
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
                    minRaise,
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
            throw new IllegalStateException("Bạn không có quyền sửa phiên đấu giá này");
        }
        if (!auction.canEdit()) {
            throw new IllegalStateException("Không thể sửa phiên đã có bid hoặc đã kết thúc");
        }
        Lock lock = lockOf(auction.getId());
        lock.lock();
        try {
            auction.getItem().setName(request.name(), now);
            auction.getItem().setDescription(request.description(), now);
            auction.getItem().setImagePath(request.imagePath(), now);
            auction.getItem().setSpecialField(request.specialField(), now);
            auction.getItem().setStartingPrice(MoneyUtils.normalize(request.startingPrice()), now);
            auction.setStartTime(request.startTime(), now);
            auction.setEndTime(request.endTime(), now);
            auction.setMinRaise(minRaise, now);
            auction.setLeader(null, null, MoneyUtils.normalize(request.startingPrice()), now);
            auction.setWalletState(false, false, now);
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
            throw new IllegalStateException("Bạn không có quyền xoá phiên đấu giá này");
        }
        if (!auction.canEdit()) {
            throw new IllegalStateException("Chỉ xoá được phiên chưa có bid");
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
            UUID previousLeaderId = auction.getLeadingBidderId();
            BigDecimal previousPrice = auction.getCurrentPrice();
            boolean previousCharged = auction.isBidderWalletCharged();
            validateManualBidFunds(auction, bidder, amount, previousLeaderId, previousPrice, previousCharged);
            try {
                rulesEngine.placeManualBid(auction, bidder, amount, now);
                syncBidWalletHold(auction, previousLeaderId, previousPrice, previousCharged, now, bidder);
                auctionDao.saveSnapshot(auction);
                return auction;
            } catch (RuntimeException exception) {
                rollbackAuction(auctionId);
                throw exception;
            }
        } finally {
            lock.unlock();
        }
    }

    public Auction configureAutoBid(User bidder, UUID auctionId, BigDecimal maxBid, BigDecimal increment, LocalDateTime now) {
        Auction auction = getAuction(auctionId);
        Lock lock = lockOf(auctionId);
        lock.lock();
        try {
            UUID previousLeaderId = auction.getLeadingBidderId();
            BigDecimal previousPrice = auction.getCurrentPrice();
            boolean previousCharged = auction.isBidderWalletCharged();
            int previousBidCount = auction.getBidHistory().size();
            validateAutoBidFunds(auction, bidder, maxBid);
            try {
                rulesEngine.configureAutoBid(auction, bidder, maxBid, increment, now);
                if (walletRelevantChange(auction, previousLeaderId, previousPrice, previousBidCount)) {
                    syncBidWalletHold(auction, previousLeaderId, previousPrice, previousCharged, now, bidder);
                }
                auctionDao.saveSnapshot(auction);
                return auction;
            } catch (RuntimeException exception) {
                rollbackAuction(auctionId);
                throw exception;
            }
        } finally {
            lock.unlock();
        }
    }

    public Auction updateStatus(User actor, UUID auctionId, AuctionStatus status, LocalDateTime now) {
        Auction auction = getAuction(auctionId);
        if (actor.getRole() != Role.ADMIN && !auction.getSellerId().equals(actor.getId())) {
            throw new IllegalStateException("Bạn không có quyền cập nhật trạng thái");
        }
        Lock lock = lockOf(auctionId);
        lock.lock();
        try {
            try {
                if (status == AuctionStatus.PAID) {
                    if (auction.getStatus() != AuctionStatus.FINISHED) {
                        throw new IllegalStateException("Only finished auctions can be marked as paid");
                    }
                    settleSellerPayout(auction, now, true, actor);
                    auction.markPaid(now);
                } else if (status == AuctionStatus.CANCELED) {
                    if (auction.isSellerWalletPaid()) {
                        throw new IllegalStateException("Phiên đã thanh toán cho seller nên không thể hủy");
                    }
                    auction.cancel(now);
                    refundBidWalletHold(auction, now, actor);
                } else {
                    throw new IllegalArgumentException("Chỉ hỗ trợ chuyển sang PAID hoặc CANCELED");
                }
                auctionDao.saveSnapshot(auction);
                return auction;
            } catch (RuntimeException exception) {
                rollbackAuction(auctionId);
                throw exception;
            }
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
                    if (auction.getStatus() == AuctionStatus.FINISHED) {
                        try {
                            settleSellerPayout(auction, now, true);
                        } catch (RuntimeException exception) {
                            System.err.println("Cannot settle wallet for auction " + auction.getId() + ": " + exception.getMessage());
                        }
                    }
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

    private void validateManualBidFunds(
            Auction auction,
            User bidder,
            BigDecimal amount,
            UUID previousLeaderId,
            BigDecimal previousPrice,
            boolean previousCharged
    ) {
        BigDecimal normalizedAmount = MoneyUtils.normalize(amount);
        User freshBidder = loadUser(bidder.getId());
        BigDecimal requiredBalance = normalizedAmount;
        if (previousCharged && bidder.getId().equals(previousLeaderId)) {
            requiredBalance = normalizedAmount.subtract(previousPrice);
        }
        if (requiredBalance.signum() < 0) {
            requiredBalance = ZERO;
        }
        if (freshBidder.getBalance().compareTo(requiredBalance) < 0) {
            throw new IllegalArgumentException("Số tiền bid vượt quá số dư khả dụng trong ví");
        }
    }

    private void validateAutoBidFunds(Auction auction, User bidder, BigDecimal maxBid) {
        BigDecimal normalizedMaxBid = MoneyUtils.normalize(maxBid);
        User freshBidder = loadUser(bidder.getId());
        BigDecimal buyingPower = freshBidder.getBalance();
        if (auction.isBidderWalletCharged()
                && bidder.getId().equals(auction.getLeadingBidderId())) {
            buyingPower = buyingPower.add(auction.getCurrentPrice());
        }
        if (normalizedMaxBid.compareTo(buyingPower) > 0) {
            throw new IllegalArgumentException("MaxBid vượt quá số dư khả dụng trong ví");
        }
    }

    private boolean walletRelevantChange(
            Auction auction,
            UUID previousLeaderId,
            BigDecimal previousPrice,
            int previousBidCount
    ) {
        return previousBidCount != auction.getBidHistory().size()
                || !Objects.equals(previousLeaderId, auction.getLeadingBidderId())
                || previousPrice.compareTo(auction.getCurrentPrice()) != 0;
    }

    private void syncBidWalletHold(
            Auction auction,
            UUID previousLeaderId,
            BigDecimal previousPrice,
            boolean previousCharged,
            LocalDateTime now,
            User... sessionUsers
    ) {
        Map<UUID, BigDecimal> deltas = new HashMap<>();
        if (previousCharged && previousLeaderId != null) {
            addDelta(deltas, previousLeaderId, previousPrice);
        }
        if (auction.getLeadingBidderId() != null) {
            addDelta(deltas, auction.getLeadingBidderId(), auction.getCurrentPrice().negate());
        }
        applyWalletDeltas(deltas, now, sessionUsers);
        auction.setWalletState(auction.getLeadingBidderId() != null, false, now);
    }

    private boolean refundBidWalletHold(Auction auction, LocalDateTime now, User... sessionUsers) {
        if (!auction.isBidderWalletCharged() || auction.getLeadingBidderId() == null) {
            return false;
        }
        Map<UUID, BigDecimal> deltas = new HashMap<>();
        addDelta(deltas, auction.getLeadingBidderId(), auction.getCurrentPrice());
        applyWalletDeltas(deltas, now, sessionUsers);
        auction.setWalletState(false, auction.isSellerWalletPaid(), now);
        return true;
    }

    private boolean settleSellerPayout(
            Auction auction,
            LocalDateTime now,
            boolean chargeWinnerIfNeeded,
            User... sessionUsers
    ) {
        if (auction.isSellerWalletPaid() || auction.getWinnerBidderId() == null) {
            return false;
        }
        Map<UUID, BigDecimal> deltas = new HashMap<>();
        if (!auction.isBidderWalletCharged()) {
            if (!chargeWinnerIfNeeded) {
                return false;
            }
            addDelta(deltas, auction.getWinnerBidderId(), auction.getCurrentPrice().negate());
        }
        addDelta(deltas, auction.getSellerId(), auction.getCurrentPrice());
        applyWalletDeltas(deltas, now, sessionUsers);
        auction.setWalletState(false, true, now);
        return true;
    }

    private void reconcileWalletStateOnStartup(LocalDateTime now) {
        for (Auction auction : auctions.values()) {
            try {
                boolean changed = false;
                if ((auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING)
                        && auction.getLeadingBidderId() != null
                        && !auction.isBidderWalletCharged()
                        && !auction.isSellerWalletPaid()) {
                    Map<UUID, BigDecimal> deltas = new HashMap<>();
                    addDelta(deltas, auction.getLeadingBidderId(), auction.getCurrentPrice().negate());
                    if (canApplyWalletDeltas(deltas)) {
                        applyWalletDeltas(deltas, now);
                        auction.setWalletState(true, false, now);
                        changed = true;
                    }
                }
                if ((auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID)
                        && !auction.isSellerWalletPaid()) {
                    changed = settleSellerPayout(auction, now, false) || changed;
                }
                if (auction.getStatus() == AuctionStatus.CANCELED && auction.isBidderWalletCharged()) {
                    changed = refundBidWalletHold(auction, now) || changed;
                }
                if (changed) {
                    auctionDao.saveSnapshot(auction);
                }
            } catch (RuntimeException exception) {
                System.err.println("Cannot reconcile wallet for auction " + auction.getId() + ": " + exception.getMessage());
            }
        }
    }

    private boolean canApplyWalletDeltas(Map<UUID, BigDecimal> deltas) {
        for (Map.Entry<UUID, BigDecimal> entry : deltas.entrySet()) {
            BigDecimal delta = MoneyUtils.normalize(entry.getValue());
            if (delta.signum() == 0) {
                continue;
            }
            User user = loadUser(entry.getKey());
            if (MoneyUtils.normalize(user.getBalance().add(delta)).signum() < 0) {
                return false;
            }
        }
        return true;
    }

    private void applyWalletDeltas(Map<UUID, BigDecimal> deltas, LocalDateTime now, User... sessionUsers) {
        if (deltas.isEmpty()) {
            return;
        }
        Map<UUID, User> users = new HashMap<>();
        Map<UUID, BigDecimal> nextBalances = new HashMap<>();
        for (Map.Entry<UUID, BigDecimal> entry : deltas.entrySet()) {
            BigDecimal delta = MoneyUtils.normalize(entry.getValue());
            if (delta.signum() == 0) {
                continue;
            }
            User user = loadUser(entry.getKey());
            BigDecimal nextBalance = MoneyUtils.normalize(user.getBalance().add(delta));
            if (nextBalance.signum() < 0) {
                throw new IllegalStateException("Số dư trong ví cua " + user.getDisplayName() + " không đủ");
            }
            users.put(entry.getKey(), user);
            nextBalances.put(entry.getKey(), nextBalance);
        }
        for (Map.Entry<UUID, User> entry : users.entrySet()) {
            User user = entry.getValue();
            user.setBalance(nextBalances.get(entry.getKey()), now);
            userDao.save(user);
        }
        syncSessionUsers(users, sessionUsers);
    }

    private void addDelta(Map<UUID, BigDecimal> deltas, UUID userId, BigDecimal delta) {
        if (userId == null || delta == null || delta.signum() == 0) {
            return;
        }
        deltas.merge(userId, MoneyUtils.normalize(delta), BigDecimal::add);
    }

    private void syncSessionUsers(Map<UUID, User> updatedUsers, User... sessionUsers) {
        if (sessionUsers == null) {
            return;
        }
        for (User sessionUser : sessionUsers) {
            if (sessionUser == null) {
                continue;
            }
            User updatedUser = updatedUsers.get(sessionUser.getId());
            if (updatedUser != null) {
                sessionUser.setBalance(updatedUser.getBalance(), updatedUser.getUpdatedAt());
            }
        }
    }

    private User loadUser(UUID userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private void rollbackAuction(UUID auctionId) {
        auctionDao.findById(auctionId).ifPresent(stored -> auctions.put(auctionId, stored));
    }

    private void ensureSeller(User seller) {
        if (seller.getRole() != Role.SELLER && seller.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Chỉ seller/admin mới được quản lý sản phẩm");
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
            throw new IllegalArgumentException("Thông tin sản phẩm không được để trống");
        }
        if (startingPrice == null || startingPrice.signum() < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không hợp lệ");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thời gian đấu giá không hợp lệ");
        }
    }

    private Lock lockOf(UUID auctionId) {
        return locks.computeIfAbsent(auctionId, key -> new ReentrantLock());
    }
}
