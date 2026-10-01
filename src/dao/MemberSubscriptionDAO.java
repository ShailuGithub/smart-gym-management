package dao;

import db.DBConnection;
import model.MemberSubscription;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberSubscriptionDAO {

    private static final String SELECT_WITH_PLAN =
            "SELECT ms.*, p.name AS plan_name FROM member_subscriptions ms "
                    + "JOIN membership_plans p ON ms.plan_id = p.plan_id ";

    /** Full renewal history for one member, newest first. */
    public List<MemberSubscription> findByMember(int memberId) throws SQLException {
        String sql = SELECT_WITH_PLAN + "WHERE ms.member_id=? ORDER BY ms.start_date DESC, ms.subscription_id DESC";
        List<MemberSubscription> list = new ArrayList<>();
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
     * The member's most relevant subscription row - the one with the latest end_date - or null
     * if they've never had one. This is what "current plan" means (see MembershipStatus); a
     * member can only have overlapping rows if renewed early, so end_date DESC picks the one
     * that determines their real expiry.
     */
    public MemberSubscription findLatestForMember(int memberId) throws SQLException {
        String sql = SELECT_WITH_PLAN + "WHERE ms.member_id=? ORDER BY ms.end_date DESC, ms.subscription_id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setMaxRows(1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Batch version of findLatestForMember() for every member in a branch, in one query - avoids
     * an N+1 per-member lookup when computing dashboard/list stats (mirrors the reference
     * project's totalPaidByStudentForMonth batch-map pattern).
     */
    public Map<Integer, MemberSubscription> findLatestForBranch(int branchId) throws SQLException {
        String sql = "SELECT ms.*, p.name AS plan_name FROM member_subscriptions ms "
                + "JOIN membership_plans p ON ms.plan_id = p.plan_id "
                + "INNER JOIN (SELECT member_id, MAX(end_date) AS max_end FROM member_subscriptions "
                + "            WHERE branch_id=? GROUP BY member_id) latest "
                + "  ON ms.member_id = latest.member_id AND ms.end_date = latest.max_end "
                + "WHERE ms.branch_id=?";
        Map<Integer, MemberSubscription> map = new HashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // A tie on max end_date (two subscriptions ending the same day) is a rare edge
                    // case - keep whichever row is seen first, it's the same expiry date either way.
                    MemberSubscription sub = map(rs);
                    map.putIfAbsent(sub.getMemberId(), sub);
                }
            }
        }
        return map;
    }

    public int insert(MemberSubscription s) throws SQLException {
        String sql = "INSERT INTO member_subscriptions (member_id, branch_id, plan_id, start_date, end_date, "
                + "amount_charged, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getMemberId());
            ps.setInt(2, s.getBranchId());
            ps.setInt(3, s.getPlanId());
            ps.setDate(4, Date.valueOf(s.getStartDate()));
            ps.setDate(5, Date.valueOf(s.getEndDate()));
            ps.setBigDecimal(6, s.getAmountCharged());
            ps.setString(7, s.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed, no generated key returned for member_subscriptions");
    }

    public void deleteByMember(int memberId) throws SQLException {
        String sql = "DELETE FROM member_subscriptions WHERE member_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.executeUpdate();
        }
    }

    private MemberSubscription map(ResultSet rs) throws SQLException {
        MemberSubscription s = new MemberSubscription();
        s.setSubscriptionId(rs.getInt("subscription_id"));
        s.setMemberId(rs.getInt("member_id"));
        s.setBranchId(rs.getInt("branch_id"));
        s.setPlanId(rs.getInt("plan_id"));
        s.setStartDate(rs.getDate("start_date").toLocalDate());
        s.setEndDate(rs.getDate("end_date").toLocalDate());
        BigDecimal amount = rs.getBigDecimal("amount_charged");
        s.setAmountCharged(amount == null ? BigDecimal.ZERO : amount);
        s.setStatus(rs.getString("status"));
        s.setPlanName(rs.getString("plan_name"));
        return s;
    }
}
