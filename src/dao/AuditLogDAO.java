package dao;

import db.DBConnection;
import model.AuditLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    /**
     * Records one audit entry. Deliberately swallows SQLException (logging to stderr
     * instead of throwing) - an audit record failing to write must never break the
     * actual business operation it's describing.
     */
    public void log(Integer branchId, Integer userId, String username, String action, String entityType,
                     Integer entityId, String description) {
        String sql = "INSERT INTO audit_log (branch_id, user_id, username, action, entity_type, entity_id, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (branchId != null) {
                ps.setInt(1, branchId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            if (userId != null) {
                ps.setInt(2, userId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, username);
            ps.setString(4, action);
            ps.setString(5, entityType);
            if (entityId != null) {
                ps.setInt(6, entityId);
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Most recent entries across every branch (ADMIN view), newest first. */
    public List<AuditLogEntry> findAll(int limit) throws SQLException {
        String sql = "SELECT TOP (?) * FROM audit_log ORDER BY log_id DESC";
        List<AuditLogEntry> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Most recent entries for one branch (MANAGER view, or ADMIN filtering to a single branch), newest first. */
    public List<AuditLogEntry> findByBranch(int branchId, int limit) throws SQLException {
        String sql = "SELECT TOP (?) * FROM audit_log WHERE branch_id=? ORDER BY log_id DESC";
        List<AuditLogEntry> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private AuditLogEntry map(ResultSet rs) throws SQLException {
        AuditLogEntry e = new AuditLogEntry();
        e.setLogId(rs.getInt("log_id"));
        int branchId = rs.getInt("branch_id");
        e.setBranchId(rs.wasNull() ? null : branchId);
        e.setUsername(rs.getString("username"));
        e.setAction(rs.getString("action"));
        e.setEntityType(rs.getString("entity_type"));
        int entityId = rs.getInt("entity_id");
        e.setEntityId(rs.wasNull() ? null : entityId);
        e.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        e.setCreatedAt(ts.toLocalDateTime());
        return e;
    }
}
