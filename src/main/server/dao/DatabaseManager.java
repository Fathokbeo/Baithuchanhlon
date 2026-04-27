package main.server.dao;

import java.sql.DriverManager;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
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
}
