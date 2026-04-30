package main.server.dao;

import main.shared.factory.ItemFactory;
import main.shared.model.Auction;
import main.shared.model.AuctionStatus;
import main.shared.model.AutoBidConfig;
import main.shared.model.BidSource;
import main.shared.model.BidTransaction;
import main.shared.model.Item;
import main.shared.model.ItemType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AuctionDao {
 private final DatabaseManager databaseManager;

 public AuctionDao(DatabaseManager databaseManager) {
  this.databaseManager = databaseManager;
 }

 public List<Auction> findAll() {
  try (Connection connection = databaseManager.getConnection();
       PreparedStatement statement = connection.prepareStatement("select * from auctions order by start_time desc")) {
   ResultSet resultSet = statement.executeQuery();
   List<Auction> auctions = new ArrayList<>();
   while (resultSet.next()) {
    auctions.add(mapAuction(connection, resultSet));
   }
   return auctions;
  } catch (SQLException exception) {
   throw new IllegalStateException("Cannot list auctions", exception);
  }
 }

 public Optional<Auction> findById(UUID auctionId) {
  try (Connection connection = databaseManager.getConnection();
       PreparedStatement statement = connection.prepareStatement("select * from auctions where id = ?")) {
   statement.setString(1, auctionId.toString());
   ResultSet resultSet = statement.executeQuery();
   if (!resultSet.next()) {
    return Optional.empty();
   }
   return Optional.of(mapAuction(connection, resultSet));
  } catch (SQLException exception) {
   throw new IllegalStateException("Cannot find auction", exception);
  }
 }

 public List<Auction> findBySellerId(UUID sellerId) {
  try (Connection connection = databaseManager.getConnection();
       PreparedStatement statement = connection.prepareStatement("""
                     select * from auctions where seller_id = ? order by start_time desc
                     """)) {
   statement.setString(1, sellerId.toString());
   ResultSet resultSet = statement.executeQuery();
   List<Auction> auctions = new ArrayList<>();
   while (resultSet.next()) {
    auctions.add(mapAuction(connection, resultSet));
   }
   return auctions;
  } catch (SQLException exception) {
   throw new IllegalStateException("Cannot list seller auctions", exception);
  }
 }

 public void saveSnapshot(Auction auction) {
  try (Connection connection = databaseManager.getConnection()) {
   connection.setAutoCommit(false);
   try {
    writeAuction(connection, auction);
    deleteChildren(connection, auction.getId());
    writeBids(connection, auction);
    writeAutoBids(connection, auction);
    connection.commit();
   } catch (SQLException exception) {
    connection.rollback();
    throw exception;
   } finally {
    connection.setAutoCommit(true);
   }
  } catch (SQLException exception) {
   throw new IllegalStateException("Cannot save auction snapshot", exception);
  }
 }

 public void deleteAuction(UUID auctionId) {
  try (Connection connection = databaseManager.getConnection()) {
   connection.setAutoCommit(false);
   try (PreparedStatement deleteBids = connection.prepareStatement("delete from bids where auction_id = ?");
        PreparedStatement deleteAutoBids = connection.prepareStatement("delete from auto_bids where auction_id = ?");
        PreparedStatement deleteAuction = connection.prepareStatement("delete from auctions where id = ?")) {
    deleteBids.setString(1, auctionId.toString());
    deleteAutoBids.setString(1, auctionId.toString());
    deleteAuction.setString(1, auctionId.toString());
    deleteBids.executeUpdate();
    deleteAutoBids.executeUpdate();
    deleteAuction.executeUpdate();
    connection.commit();
   } catch (SQLException exception) {
    connection.rollback();
    throw exception;
   } finally {
    connection.setAutoCommit(true);
   }
  } catch (SQLException exception) {
   throw new IllegalStateException("Cannot delete auction", exception);
  }
 }

 private void writeAuction(Connection connection, Auction auction) throws SQLException {
  try (PreparedStatement statement = connection.prepareStatement("""
                merge into auctions (
                    id, seller_id, item_id, item_type, item_name, item_description, starting_price,
                    current_price, special_field, leading_bidder_id, leading_bidder_name, winner_bidder_id,
                    winner_bidder_name, status, start_time, end_time, extension_count, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
   Item item = auction.getItem();
   statement.setString(1, auction.getId().toString());
   statement.setString(2, auction.getSellerid().toString());
   statement.setString(3, item.getId().toString());
   statement.setString(4, item.getType().name());
   statement.setString(5, item.getName());
   statement.setString(6, item.getDescription());
   statement.setBigDecimal(7, item.getStartingprice());
   statement.setBigDecimal(8, auction.getCurrentprice());
   statement.setString(9, item.getSpecialfield());
   statement.setString(10, auction.getLeadingBidderId() == null ? null : auction.getLeadingBidderId().toString());
   statement.setString(11, auction.getLeadingBiddername());
   statement.setString(12, auction.getWinnerBidderId() == null ? null : auction.getWinnerBidderId().toString());
   statement.setString(13, auction.getWinnerBiddername());
   statement.setString(14, auction.getStatus().name());
   statement.setTimestamp(15, Timestamp.valueOf(auction.getStartTime()));
   statement.setTimestamp(16, Timestamp.valueOf(auction.getEndTime()));
   statement.setInt(17, auction.getExtentionCount());
   statement.setTimestamp(18, Timestamp.valueOf(auction.getCreateAt()));
   statement.setTimestamp(19, Timestamp.valueOf(auction.getUpdateAt()));
   statement.executeUpdate();
  }
 }

 private void deleteChildren(Connection connection, UUID auctionId) throws SQLException {
  try (PreparedStatement deleteBids = connection.prepareStatement("delete from bids where auction_id = ?");
       PreparedStatement deleteAutoBids = connection.prepareStatement("delete from auto_bids where auction_id = ?")) {
   deleteBids.setString(1, auctionId.toString());
   deleteAutoBids.setString(1, auctionId.toString());
   deleteBids.executeUpdate();
   deleteAutoBids.executeUpdate();
  }
 }

 private void writeBids(Connection connection, Auction auction) throws SQLException {
  try (PreparedStatement statement = connection.prepareStatement("""
                insert into bids (id, auction_id, bidder_id, bidder_name, amount, bid_time, source, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
   for (BidTransaction bid : auction.getBidHistory()) {
    statement.setString(1, bid.getId().toString());
    statement.setString(2, auction.getId().toString());
    statement.setString(3, bid.getBidderId().toString());
    statement.setString(4, bid.getBidderName());
    statement.setBigDecimal(5, bid.getAmount());
    statement.setTimestamp(6, Timestamp.valueOf(bid.getBidTime()));
    statement.setString(7, bid.getSource().name());
    statement.setTimestamp(8, Timestamp.valueOf(bid.getCreateAt()));
    statement.setTimestamp(9, Timestamp.valueOf(bid.getUpdateAt()));
    statement.addBatch();
   }
   statement.executeBatch();
  }
 }

 private void writeAutoBids(Connection connection, Auction auction) throws SQLException {
  try (PreparedStatement statement = connection.prepareStatement("""
                insert into auto_bids (
                    id, auction_id, bidder_id, bidder_name, max_bid, increment_amount, registered_at, active, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
   for (AutoBidConfig config : auction.getAutoBidConfigs()) {
    statement.setString(1, config.getId().toString());
    statement.setString(2, auction.getId().toString());
    statement.setString(3, config.getBidderId().toString());
    statement.setString(4, config.getBidderName());
    statement.setBigDecimal(5, config.getMaxBid());
    statement.setBigDecimal(6, config.getIncrement());
    statement.setTimestamp(7, Timestamp.valueOf(config.getRegisteredAt()));
    statement.setBoolean(8, config.isActive());
    statement.setTimestamp(9, Timestamp.valueOf(config.getCreateAt()));
    statement.setTimestamp(10, Timestamp.valueOf(config.getUpdateAt()));
    statement.addBatch();
   }
   statement.executeBatch();
  }
 }

 private Auction mapAuction(Connection connection, ResultSet resultSet) throws SQLException {
  UUID auctionId = UUID.fromString(resultSet.getString("id"));
  UUID sellerId = UUID.fromString(resultSet.getString("seller_id"));
  UUID itemId = UUID.fromString(resultSet.getString("item_id"));
  LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
  LocalDateTime updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime();
  Item item = ItemFactory.create(
          ItemType.valueOf(resultSet.getString("item_type")),
          itemId,
          createdAt,
          updatedAt,
          sellerId,
          resultSet.getString("item_name"),
          resultSet.getString("item_description"),
          resultSet.getBigDecimal("starting_price"),
          resultSet.getString("special_field")
  );
  return new Auction(
          auctionId,
          createdAt,
          updatedAt,
          item,
          sellerId,
          resultSet.getBigDecimal("current_price"),
          uuidOrNull(resultSet.getString("leading_bidder_id")),
          resultSet.getString("leading_bidder_name"),
          uuidOrNull(resultSet.getString("winner_bidder_id")),
          resultSet.getString("winner_bidder_name"),
          AuctionStatus.valueOf(resultSet.getString("status")),
          resultSet.getTimestamp("start_time").toLocalDateTime(),
          resultSet.getTimestamp("end_time").toLocalDateTime(),
          resultSet.getInt("extension_count"),
          loadBids(connection, auctionId),
          loadAutoBids(connection, auctionId)
  );
 }

 private List<BidTransaction> loadBids(Connection connection, UUID auctionId) throws SQLException {
  try (PreparedStatement statement = connection.prepareStatement("""
                select * from bids where auction_id = ? order by bid_time
                """)) {
   statement.setString(1, auctionId.toString());
   ResultSet resultSet = statement.executeQuery();
   List<BidTransaction> bids = new ArrayList<>();
   while (resultSet.next()) {
    LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
    bids.add(new BidTransaction(
            UUID.fromString(resultSet.getString("id")),
            createdAt,
            resultSet.getTimestamp("updated_at").toLocalDateTime(),
            auctionId,
            UUID.fromString(resultSet.getString("bidder_id")),
            resultSet.getString("bidder_name"),
            resultSet.getBigDecimal("amount"),
            resultSet.getTimestamp("bid_time").toLocalDateTime(),
            BidSource.valueOf(resultSet.getString("source"))
    ));
   }
   return bids;
  }
 }

 private List<AutoBidConfig> loadAutoBids(Connection connection, UUID auctionId) throws SQLException {
  try (PreparedStatement statement = connection.prepareStatement("""
                select * from auto_bids where auction_id = ? order by registered_at
                """)) {
   statement.setString(1, auctionId.toString());
   ResultSet resultSet = statement.executeQuery();
   List<AutoBidConfig> configs = new ArrayList<>();
   while (resultSet.next()) {
    LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
    configs.add(new AutoBidConfig(
            UUID.fromString(resultSet.getString("id")),
            createdAt,
            resultSet.getTimestamp("updated_at").toLocalDateTime(),
            auctionId,
            UUID.fromString(resultSet.getString("bidder_id")),
            resultSet.getString("bidder_name"),
            resultSet.getBigDecimal("max_bid"),
            resultSet.getBigDecimal("increment_amount"),
            resultSet.getTimestamp("registered_at").toLocalDateTime(),
            resultSet.getBoolean("active")
    ));
   }
   return configs;
  }
 }

 private UUID uuidOrNull(String value) {
  return value == null ? null : UUID.fromString(value);
 }
}