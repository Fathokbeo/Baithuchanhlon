package main.server.seed;

import main.server.dao.AuctionDao;
import main.server.dao.UserDao;
import main.shared.factory.ItemFactory;
import main.shared.model.Admin;
import main.shared.model.Auction;
import main.shared.model.AuctionStatus;
import main.shared.model.BidSource;
import main.shared.model.BidTransaction;
import main.shared.model.Bidder;
import main.shared.model.Item;
import main.shared.model.ItemType;
import main.shared.model.Seller;
import main.shared.model.User;
import main.shared.util.PasswordUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class DataSeeder {
    private final UserDao userDao;
    private final AuctionDao auctionDao;

    public DataSeeder(UserDao userDao, AuctionDao auctionDao) {
        this.userDao = userDao;
        this.auctionDao = auctionDao;
    }

    public void seedIfEmpty() {
        if (userDao.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Seller seller = new Seller(UUID.randomUUID(), now, now, "seller1", PasswordUtils.hash("seller123"), "Nguyen Seller");
        Bidder bidder1 = new Bidder(UUID.randomUUID(), now, now, "bidder1", PasswordUtils.hash("bid123"), "Tran Bidder");
        Bidder bidder2 = new Bidder(UUID.randomUUID(), now, now, "bidder2", PasswordUtils.hash("bid123"), "Le AutoBid");
        Admin admin = new Admin(UUID.randomUUID(), now, now, "admin", PasswordUtils.hash("admin123"), "System Admin");

        List<User> users = List.of(seller, bidder1, bidder2, admin);
        users.forEach(userDao::save);

        auctionDao.saveSnapshot(createRunningElectronicsAuction(now, seller, bidder1));
        auctionDao.saveSnapshot(createOpenArtAuction(now, seller));
        auctionDao.saveSnapshot(createFinishedVehicleAuction(now, seller, bidder2));
    }

    private Auction createRunningElectronicsAuction(LocalDateTime now, Seller seller, Bidder bidder) {
        Item item = ItemFactory.create(
                ItemType.ELECTRONICS,
                UUID.randomUUID(),
                now.minusMinutes(5),
                now.minusMinutes(5),
                seller.getId(),
                "MacBook Pro M4",
                "Laptop phuc vu developer, RAM 24GB, SSD 1TB.",
                new BigDecimal("1500.00"),
                "Apple | Bao hanh 12 thang"
        );
        BidTransaction bid = new BidTransaction(
                UUID.randomUUID(),
                now.minusMinutes(2),
                now.minusMinutes(2),
                UUID.randomUUID(),
                bidder.getId(),
                bidder.getDisplayName(),
                new BigDecimal("1650.00"),
                now.minusMinutes(2),
                BidSource.MANUAL
        );
        return new Auction(
                bid.getAuctionId(),
                now.minusMinutes(5),
                now.minusMinutes(2),
                item,
                seller.getId(),
                bid.getAmount(),
                bidder.getId(),
                bidder.getDisplayName(),
                null,
                null,
                AuctionStatus.RUNNING,
                now.minusMinutes(5),
                now.plusMinutes(10),
                0,
                List.of(bid),
                List.of()
        );
    }

    private Auction createOpenArtAuction(LocalDateTime now, Seller seller) {
        Item item = ItemFactory.create(
                ItemType.ART,
                UUID.randomUUID(),
                now,
                now,
                seller.getId(),
                "Tranh Son Dau Pho Co",
                "Tac pham son dau chu de pho co Ha Noi, khung go lim.",
                new BigDecimal("800.00"),
                "Hoa si noi dia | Canvas 80x120"
        );
        return new Auction(
                UUID.randomUUID(),
                now,
                now,
                item,
                seller.getId(),
                item.getStartingPrice(),
                null,
                null,
                null,
                null,
                AuctionStatus.OPEN,
                now.plusMinutes(2),
                now.plusMinutes(25),
                0,
                List.of(),
                List.of()
        );
    }

    private Auction createFinishedVehicleAuction(LocalDateTime now, Seller seller, Bidder winner) {
        Item item = ItemFactory.create(
                ItemType.VEHICLE,
                UUID.randomUUID(),
                now.minusHours(2),
                now.minusHours(2),
                seller.getId(),
                "Honda SH 150i 2024",
                "Xe moi 95%, odo 3.500 km, chinh chu.",
                new BigDecimal("3500.00"),
                "SH 150i | 3.500km"
        );
        BidTransaction bid = new BidTransaction(
                UUID.randomUUID(),
                now.minusMinutes(70),
                now.minusMinutes(70),
                UUID.randomUUID(),
                winner.getId(),
                winner.getDisplayName(),
                new BigDecimal("3900.00"),
                now.minusMinutes(70),
                BidSource.MANUAL
        );
        return new Auction(
                bid.getAuctionId(),
                now.minusHours(2),
                now.minusMinutes(60),
                item,
                seller.getId(),
                bid.getAmount(),
                winner.getId(),
                winner.getDisplayName(),
                winner.getId(),
                winner.getDisplayName(),
                AuctionStatus.FINISHED,
                now.minusHours(2),
                now.minusMinutes(65),
                0,
                List.of(bid),
                List.of()
        );
    }
}