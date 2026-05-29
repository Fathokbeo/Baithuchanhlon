package main.server.service;
import main.server.dao.UserDao;
import main.shared.model.Admin;
import main.shared.model.Bidder;
import main.shared.model.Role;
import main.shared.model.Seller;
import main.shared.model.User;
import main.shared.util.PasswordUtils;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AuthService {
    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User login(String username, String password) {
        return userDao.findByUsername(username)
                .filter(user -> PasswordUtils.matches(password, user.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu"));
    }

    public User register(String username, String password, String displayName, Role role) {
        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("Không thể tự đăng ký tài khoản admin");
        }
        userDao.findByUsername(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        });
        LocalDateTime now = LocalDateTime.now();
        User user = switch (role) {
            case BIDDER -> new Bidder(UUID.randomUUID(), now, now, username, PasswordUtils.hash(password), displayName, BigDecimal.ZERO);
            case SELLER -> new Seller(UUID.randomUUID(), now, now, username, PasswordUtils.hash(password), displayName, BigDecimal.ZERO);
            default -> throw new IllegalArgumentException("Invalid role for registration");
        };
        userDao.save(user);
        return user;
    }

    public List<User> listUsers(User requester) {
        if (requester.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Chỉ admin mới xem được danh sách người dùng");
        }
        return userDao.findAll();
    }

    public User updateUserInfo(User user, String displayName, BigDecimal balance, String email, String phone, String address) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống");
        }
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException("Số dư không hợp lệ");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setDisplayName(displayName, now);
        user.setBalance(balance, now);
        user.setContactInfo(email, phone, address, now);
        userDao.save(user);
        return user;
    }

    public User getById(UUID userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
