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
                .orElseThrow(() -> new IllegalArgumentException("Sai ten dang nhap hoac mat khau"));
    }

    public User register(String username, String password, String displayName, Role role) {
        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("Khong the tu dang ky tai khoan admin");
        }
        userDao.findByUsername(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Ten dang nhap da ton tai");
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
            throw new IllegalStateException("Chi admin moi xem duoc danh sach nguoi dung");
        }
        return userDao.findAll();
    }

    public User updateUserInfo(User user, String displayName, BigDecimal balance, String email, String phone, String address) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Ten hien thi khong duoc de trong");
        }
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException("So du khong hop le");
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
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));
    }
}
