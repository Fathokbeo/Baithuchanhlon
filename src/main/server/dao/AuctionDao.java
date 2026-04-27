package main.server.dao;

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
