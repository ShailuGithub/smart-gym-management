package dao;

import db.DBConnection;
import model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private static final String SELECT_WITH_MEMBER =
            "SELECT p.*, m.full_name AS member_name FROM payments p JOIN members m ON p.member_id = m.member_id ";

    /** All payments for a branch, newest first - the branch's payments ledger. */
    public List<Payment> findByBranch(int branchId) throws SQLException {
        String sql = SELECT_WITH_MEMBER + "WHERE p.branch_id=? ORDER BY p.payment_date DESC, p.payment_id DESC";
        List<Payment> list = new ArrayList<>();
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

    /** A branch's payments within a date range (inclusive) - the basis for revenue reports. */
    public List<Payment> findByBranchAndDateRange(int branchId, LocalDate start, LocalDate end) throws SQLException {
        String sql = SELECT_WITH_MEMBER
                + "WHERE p.branch_id=? AND p.payment_date BETWEEN ? AND ? ORDER BY p.payment_date DESC, p.payment_id DESC";
        List<Payment> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Full payment history for one member, newest first - shown alongside their subscription history. */
    public List<Payment> findByMember(int memberId) throws SQLException {
        String sql = SELECT_WITH_MEMBER + "WHERE p.member_id=? ORDER BY p.payment_date DESC, p.payment_id DESC";
        List<Payment> list = new ArrayList<>();
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

    /** Total collected for a branch on a single calendar day - the Dashboard's "Today's Collection". */
    public BigDecimal totalForBranchAndDate(int branchId, LocalDate date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM payments WHERE branch_id=? AND payment_date=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        }
    }

    /** Total collected for a branch within a date range (inclusive) - the basis for revenue reports. */
    public BigDecimal totalForBranchAndDateRange(int branchId, LocalDate start, LocalDate end) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM payments WHERE branch_id=? AND payment_date BETWEEN ? AND ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        }
    }

    public int insert(Payment p) throws SQLException {
        String sql = "INSERT INTO payments (branch_id, member_id, subscription_id, payment_date, amount, mode, reference_no, remarks) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getBranchId());
            ps.setInt(2, p.getMemberId());
            if (p.getSubscriptionId() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, p.getSubscriptionId());
            }
            ps.setDate(4, Date.valueOf(p.getPaymentDate()));
            ps.setBigDecimal(5, p.getAmount());
            ps.setString(6, p.getMode());
            ps.setString(7, p.getReferenceNo());
            ps.setString(8, p.getRemarks());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed, no generated key returned for payments");
    }

    public Payment findById(int paymentId) throws SQLException {
        String sql = SELECT_WITH_MEMBER + "WHERE p.payment_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Purges a member's whole payment history - called before hard-deleting the member itself. */
    public void deleteByMember(int memberId) throws SQLException {
        String sql = "DELETE FROM payments WHERE member_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.executeUpdate();
        }
    }

    public void delete(int branchId, int paymentId) throws SQLException {
        String sql = "DELETE FROM payments WHERE payment_id=? AND branch_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setBranchId(rs.getInt("branch_id"));
        p.setMemberId(rs.getInt("member_id"));
        int subId = rs.getInt("subscription_id");
        p.setSubscriptionId(rs.wasNull() ? null : subId);
        p.setPaymentDate(rs.getDate("payment_date").toLocalDate());
        BigDecimal amount = rs.getBigDecimal("amount");
        p.setAmount(amount == null ? BigDecimal.ZERO : amount);
        p.setMode(rs.getString("mode"));
        p.setReferenceNo(rs.getString("reference_no"));
        p.setRemarks(rs.getString("remarks"));
        p.setMemberName(rs.getString("member_name"));
        return p;
    }
}
