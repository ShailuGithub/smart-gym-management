package dao;

import db.DBConnection;
import model.Reminder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReminderDAO {

    public int insert(Reminder r) throws SQLException {
        String sql = "INSERT INTO reminders (member_id, branch_id, type, channel, message, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getMemberId());
            ps.setInt(2, r.getBranchId());
            ps.setString(3, r.getType());
            ps.setString(4, r.getChannel());
            ps.setString(5, r.getMessage());
            ps.setString(6, r.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed, no generated key returned for reminders");
    }

    /** Full reminder history for one member, newest first. */
    public List<Reminder> findByMember(int memberId) throws SQLException {
        String sql = "SELECT * FROM reminders WHERE member_id=? ORDER BY sent_at DESC";
        List<Reminder> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Most recent reminder for each member in a branch, in one query - avoids an N+1 per-member
     * lookup on the Reminders list screen (same batch-map pattern as
     * MemberSubscriptionDAO.findLatestForBranch).
     */
    public Map<Integer, Reminder> findLatestForBranch(int branchId) throws SQLException {
        String sql = "SELECT r.* FROM reminders r "
                + "INNER JOIN (SELECT member_id, MAX(sent_at) AS max_sent FROM reminders "
                + "            WHERE branch_id=? GROUP BY member_id) latest "
                + "  ON r.member_id = latest.member_id AND r.sent_at = latest.max_sent "
                + "WHERE r.branch_id=?";
        Map<Integer, Reminder> map = new HashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reminder r = map(rs);
                    map.putIfAbsent(r.getMemberId(), r);
                }
            }
        }
        return map;
    }

    /**
     * True if this member already got a reminder today - used to skip them during "Send to All"
     * so repeated clicks (or running it daily) don't spam the same member; an explicit single
     * "Send" always goes through regardless, since that's a deliberate staff action.
     */
    public boolean remindedToday(int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reminders WHERE member_id=? AND CAST(sent_at AS DATE) = CAST(GETDATE() AS DATE)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Reminder map(ResultSet rs) throws SQLException {
        Reminder r = new Reminder();
        r.setReminderId(rs.getInt("reminder_id"));
        r.setMemberId(rs.getInt("member_id"));
        r.setBranchId(rs.getInt("branch_id"));
        r.setType(rs.getString("type"));
        r.setChannel(rs.getString("channel"));
        r.setMessage(rs.getString("message"));
        r.setStatus(rs.getString("status"));
        r.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return r;
    }
}
