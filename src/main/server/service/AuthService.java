package main.server.service;
import main.server.dao.UserDao;
import main.shared.model.Admin;
import main.shared.model.Bidder;
import main.shared.model.Role;
import main.shared.model.Seller;
import main.shared.model.User;
import main.shared.util.PasswordUtils;

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
            case BIDDER -> new Bidder(UUID.randomUUID(), now, now, username, PasswordUtils.hash(password), displayName);
            case SELLER -> new Seller(UUID.randomUUID(), now, now, username, PasswordUtils.hash(password), displayName);
            case ADMIN -> new Admin(UUID.randomUUID(), now, now, username, PasswordUtils.hash(password), displayName);
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

    public User getById(UUID userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));
    }
}