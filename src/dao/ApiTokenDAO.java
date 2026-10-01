package dao;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Opaque bearer tokens issued to the Flutter app on login (see api.AuthApiServlet) - mobile
 * clients are stateless, so there's no HttpSession to hold the logged-in user the way the web
 * app does; this table plays that role instead.
 */
public class ApiTokenDAO {

    /** One resolved token row: which user it belongs to, and whether it's still valid. */
    public static final class TokenInfo {
        private final int userId;
        private final LocalDateTime expiresAt;

        TokenInfo(int userId, LocalDateTime expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }

        public int getUserId() {
            return userId;
        }

        public boolean isExpired() {
            return expiresAt.isBefore(LocalDateTime.now());
        }
    }

    public void insert(int userId, String token, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO api_tokens (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    /** Returns the token's owner/expiry, or null if the token doesn't exist at all. */
    public TokenInfo findByToken(String token) throws SQLException {
        String sql = "SELECT user_id, expires_at FROM api_tokens WHERE token=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new TokenInfo(rs.getInt("user_id"), rs.getTimestamp("expires_at").toLocalDateTime());
            }
        }
    }

    /** Invalidates one token - used on logout. */
    public void delete(String token) throws SQLException {
        String sql = "DELETE FROM api_tokens WHERE token=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    /** Housekeeping - not called automatically anywhere yet, available for a future scheduled cleanup. */
    public void deleteExpired() throws SQLException {
        String sql = "DELETE FROM api_tokens WHERE expires_at < GETDATE()";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
