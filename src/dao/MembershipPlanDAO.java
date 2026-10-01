package dao;

import db.DBConnection;
import model.MembershipPlan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MembershipPlanDAO {

    public List<MembershipPlan> findAll() throws SQLException {
        String sql = "SELECT * FROM membership_plans ORDER BY duration_days";
        List<MembershipPlan> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** ACTIVE plans only - for the "assign/renew plan" dropdown on a member. */
    public List<MembershipPlan> findAllActive() throws SQLException {
        String sql = "SELECT * FROM membership_plans WHERE status='ACTIVE' ORDER BY duration_days";
        List<MembershipPlan> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public MembershipPlan findById(int planId) throws SQLException {
        String sql = "SELECT * FROM membership_plans WHERE plan_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(MembershipPlan p) throws SQLException {
        String sql = "INSERT INTO membership_plans (name, duration_days, price, description, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed, no generated key returned for membership_plans");
    }

    public void update(MembershipPlan p) throws SQLException {
        String sql = "UPDATE membership_plans SET name=?, duration_days=?, price=?, description=?, status=? "
                + "WHERE plan_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, p);
            ps.setInt(6, p.getPlanId());
            ps.executeUpdate();
        }
    }

    public void delete(int planId) throws SQLException {
        String sql = "DELETE FROM membership_plans WHERE plan_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, MembershipPlan p) throws SQLException {
        ps.setString(1, p.getName());
        ps.setInt(2, p.getDurationDays());
        ps.setBigDecimal(3, p.getPrice());
        ps.setString(4, p.getDescription());
        ps.setString(5, p.getStatus());
    }

    private MembershipPlan map(ResultSet rs) throws SQLException {
        MembershipPlan p = new MembershipPlan();
        p.setPlanId(rs.getInt("plan_id"));
        p.setName(rs.getString("name"));
        p.setDurationDays(rs.getInt("duration_days"));
        BigDecimal price = rs.getBigDecimal("price");
        p.setPrice(price == null ? BigDecimal.ZERO : price);
        p.setDescription(rs.getString("description"));
        p.setStatus(rs.getString("status"));
        return p;
    }
}
