package dao;

import db.DBConnection;
import model.GymBranch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GymBranchDAO {

    public List<GymBranch> findAll() throws SQLException {
        String sql = "SELECT * FROM gym_branches ORDER BY name";
        List<GymBranch> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public GymBranch findById(int branchId) throws SQLException {
        String sql = "SELECT * FROM gym_branches WHERE branch_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(GymBranch b) throws SQLException {
        String sql = "INSERT INTO gym_branches (name, address, city, state, phone, email, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, b);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed, no generated key returned for gym_branches");
    }

    public void update(GymBranch b) throws SQLException {
        String sql = "UPDATE gym_branches SET name=?, address=?, city=?, state=?, phone=?, email=?, status=? "
                + "WHERE branch_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, b);
            ps.setInt(8, b.getBranchId());
            ps.executeUpdate();
        }
    }

    public void delete(int branchId) throws SQLException {
        String sql = "DELETE FROM gym_branches WHERE branch_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, GymBranch b) throws SQLException {
        ps.setString(1, b.getName());
        ps.setString(2, b.getAddress());
        ps.setString(3, b.getCity());
        ps.setString(4, b.getState());
        ps.setString(5, b.getPhone());
        ps.setString(6, b.getEmail());
        ps.setString(7, b.getStatus());
    }

    private GymBranch map(ResultSet rs) throws SQLException {
        GymBranch b = new GymBranch();
        b.setBranchId(rs.getInt("branch_id"));
        b.setName(rs.getString("name"));
        b.setAddress(rs.getString("address"));
        b.setCity(rs.getString("city"));
        b.setState(rs.getString("state"));
        b.setPhone(rs.getString("phone"));
        b.setEmail(rs.getString("email"));
        b.setStatus(rs.getString("status"));
        return b;
    }
}
