package main.server.dao;


import main.shared.model.Admin;
import main.shared.model.Bidder;
import main.shared.model.Role;
import main.shared.model.Seller;
import main.shared.model.User;

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

public final class UserDao {
    private final DatabaseManager databaseManager;

    public UserDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(User user) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     merge into users (id, username, password_hash, display_name, role, created_at, updated_at)
                     values (?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, user.getId().toString());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getDisplayName());
            statement.setString(5, user.getRole().name());
            statement.setTimestamp(6, Timestamp.valueOf(user.getCreateAt()));
            statement.setTimestamp(7, Timestamp.valueOf(user.getUpdateAt()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot save user", exception);
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("select * from users where username = ?")) {
            statement.setString(1, username);
            return readSingle(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot find user by username", exception);
        }
    }

    public Optional<User> findById(UUID userId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("select * from users where id = ?")) {
            statement.setString(1, userId.toString());
            return readSingle(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot find user by id", exception);
        }
    }

    public List<User> findAll() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("select * from users order by created_at")) {
            ResultSet resultSet = statement.executeQuery();
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot list users", exception);
        }
    }

    public long count() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("select count(*) from users")) {
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot count users", exception);
        }
    }

    private Optional<User> readSingle(PreparedStatement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            return Optional.empty();
        }
        return Optional.of(mapUser(resultSet));
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime();
        String username = resultSet.getString("username");
        String passwordHash = resultSet.getString("password_hash");
        String displayName = resultSet.getString("display_name");
        Role role = Role.valueOf(resultSet.getString("role"));
        return switch (role) {
            case BIDDER -> new Bidder(id, createdAt, updatedAt, username, passwordHash, displayName);
            case SELLER -> new Seller(id, createdAt, updatedAt, username, passwordHash, displayName);
            case ADMIN -> new Admin(id, createdAt, updatedAt, username, passwordHash, displayName);
        };
    }
}