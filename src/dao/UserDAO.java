package dao;

import db.DBConnection;
import model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Complements RateLimiter's per-IP throttling on /login - this instead stops one specific
    // account being brute-forced regardless of how many different addresses the attempts come from.
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    /** Thrown by authenticate() when the matched account is currently locked out. */
    public static final class AccountLockedException extends SQLException {
        private final int minutesRemaining;

        AccountLockedException(int minutesRemaining) {
            super("Account locked for " + minutesRemaining + " more minute(s)");
            this.minutesRemaining = minutesRemaining;
        }

        public int getMinutesRemaining() {
            return minutesRemaining;
        }
    }

    /** Returns the matching user, or null if the credentials are invalid. */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (!checkPasswordAndTrackAttempts(rs, password)) {
                        return null;
                    }
                    return map(rs);
                }
            }
        }
        return null;
    }

    private boolean checkPasswordAndTrackAttempts(ResultSet rs, String password) throws SQLException {
        int userId = rs.getInt("user_id");
        Timestamp lockedUntilTs = rs.getTimestamp("locked_until");
        if (lockedUntilTs != null) {
            LocalDateTime lockedUntil = lockedUntilTs.toLocalDateTime();
            if (lockedUntil.isAfter(LocalDateTime.now())) {
                long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), lockedUntil) + 1;
                throw new AccountLockedException((int) minutesLeft);
            }
        }
        if (!BCrypt.checkpw(password, rs.getString("password"))) {
            recordFailedAttempt(userId, rs.getInt("failed_login_attempts"));
            return false;
        }
        resetFailedAttempts(userId);
        return true;
    }

    private void recordFailedAttempt(int userId, int currentFailedAttempts) throws SQLException {
        int newCount = currentFailedAttempts + 1;
        boolean lockNow = newCount >= MAX_FAILED_ATTEMPTS;
        String sql = lockNow
                ? "UPDATE users SET failed_login_attempts=?, locked_until=DATEADD(MINUTE, ?, GETDATE()) WHERE user_id=?"
                : "UPDATE users SET failed_login_attempts=? WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (lockNow) {
                ps.setInt(1, newCount);
                ps.setInt(2, LOCKOUT_MINUTES);
                ps.setInt(3, userId);
            } else {
                ps.setInt(1, newCount);
                ps.setInt(2, userId);
            }
            ps.executeUpdate();
        }
    }

    /** Clears any failed-attempt count/lockout for this user - called on successful login and password reset. */
    public void resetFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts=0, locked_until=NULL WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Looks up a user by username with no password check - used by the forgot-password flow. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean verifyPassword(int userId, String plainPassword) throws SQLException {
        String sql = "SELECT password FROM users WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return BCrypt.checkpw(plainPassword, rs.getString("password"));
            }
        }
    }

    public void updatePassword(int userId, String newPlainPassword) throws SQLException {
        String sql = "UPDATE users SET password=? WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPlainPassword, BCrypt.gensalt()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Every user account (ADMIN + MANAGER + STAFF), for the "Manage Users" screen. */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY CASE role WHEN 'ADMIN' THEN 0 ELSE 1 END, username";
        List<User> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** MANAGER/STAFF accounts assigned to one branch. */
    public List<User> findByBranch(int branchId) throws SQLException {
        String sql = "SELECT * FROM users WHERE branch_id=? ORDER BY username";
        List<User> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Creates a new user account. branchId is null for ADMIN, required for MANAGER/STAFF. Returns the new user_id. */
    public int insert(String username, String plainPassword, String fullName, String email, String role,
                       Integer branchId, boolean canCreate, boolean canEdit, boolean canDelete) throws SQLException {
        String sql = "INSERT INTO users (branch_id, username, password, full_name, email, role, can_create, can_edit, can_delete) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (branchId != null) {
                ps.setInt(1, branchId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, username);
            ps.setString(3, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
            ps.setString(4, fullName);
            ps.setString(5, email);
            ps.setString(6, role);
            ps.setBoolean(7, canCreate);
            ps.setBoolean(8, canEdit);
            ps.setBoolean(9, canDelete);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed, no generated key returned for user");
    }

    /** Updates a user's profile fields and (for MANAGER/STAFF accounts) their branch assignment/permission flags. */
    public void update(int userId, String fullName, String email, Integer branchId,
                        boolean canCreate, boolean canEdit, boolean canDelete) throws SQLException {
        String sql = "UPDATE users SET full_name=?, email=?, branch_id=?, can_create=?, can_edit=?, can_delete=? "
                + "WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            if (branchId != null) {
                ps.setInt(3, branchId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setBoolean(4, canCreate);
            ps.setBoolean(5, canEdit);
            ps.setBoolean(6, canDelete);
            ps.setInt(7, userId);
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        int branchId = rs.getInt("branch_id");
        u.setBranchId(rs.wasNull() ? null : branchId);
        u.setPermCreate(rs.getBoolean("can_create"));
        u.setPermEdit(rs.getBoolean("can_edit"));
        u.setPermDelete(rs.getBoolean("can_delete"));
        u.setEmail(rs.getString("email"));
        return u;
    }
}
