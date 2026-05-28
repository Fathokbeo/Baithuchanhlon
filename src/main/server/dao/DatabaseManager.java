package main.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String DEFAULT_URL = "jdbc:h2:file:./data/auction-db;AUTO_SERVER=TRUE";
    private static final DatabaseManager INSTANCE = new DatabaseManager(DEFAULT_URL);

    private final String jdbcUrl;

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    public void initializeSchema() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table if not exists users (
                        id varchar(36) primary key,
                        username varchar(100) not null unique,
                        password_hash varchar(255) not null,
                        display_name varchar(120) not null,
                        role varchar(20) not null,
                        balance decimal(19,2) not null default 0,
                        email varchar(255) not null default '',
                        phone varchar(50) not null default '',
                        address varchar(1000) not null default '',
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            try {
                statement.execute("alter table users add column balance decimal(19,2) not null default 0");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            try {
                statement.execute("alter table users add column email varchar(255) not null default ''");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            try {
                statement.execute("alter table users add column phone varchar(50) not null default ''");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            try {
                statement.execute("alter table users add column address varchar(1000) not null default ''");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            statement.execute("""
                    create table if not exists auctions (
                        id varchar(36) primary key,
                        seller_id varchar(36) not null,
                        item_id varchar(36) not null,
                        item_type varchar(20) not null,
                        item_name varchar(150) not null,
                        item_description clob not null,
                        item_image_path varchar(500),
                        starting_price decimal(19,2) not null,
                        current_price decimal(19,2) not null,
                        special_field varchar(255) not null,
                        leading_bidder_id varchar(36),
                        leading_bidder_name varchar(120),
                        winner_bidder_id varchar(36),
                        winner_bidder_name varchar(120),
                        status varchar(20) not null,
                        bidder_wallet_charged boolean not null default false,
                        seller_wallet_paid boolean not null default false,
                        start_time timestamp not null,
                        end_time timestamp not null,
                        extension_count int not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            try {
                statement.execute("alter table auctions add column item_image_path varchar(500)");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            try {
                statement.execute("alter table auctions add column min_rate decimal(19,2) not null default 0");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            try {
                statement.execute("alter table auctions add column bidder_wallet_charged boolean not null default false");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            try {
                statement.execute("alter table auctions add column seller_wallet_paid boolean not null default false");
            } catch (SQLException ignored) {
                // Column already exists.
            }
            statement.execute("""
                    create table if not exists bids (
                        id varchar(36) primary key,
                        auction_id varchar(36) not null,
                        bidder_id varchar(36) not null,
                        bidder_name varchar(120) not null,
                        amount decimal(19,2) not null,
                        bid_time timestamp not null,
                        source varchar(20) not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table if not exists auto_bids (
                        id varchar(36) primary key,
                        auction_id varchar(36) not null,
                        bidder_id varchar(36) not null,
                        bidder_name varchar(120) not null,
                        max_bid decimal(19,2) not null,
                        increment_amount decimal(19,2) not null,
                        registered_at timestamp not null,
                        active boolean not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot initialize database schema", exception);
        }
    }
}
