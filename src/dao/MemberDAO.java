package dao;

import db.DBConnection;
import model.Member;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public List<Member> findByBranch(int branchId) throws SQLException {
        String sql = "SELECT * FROM members WHERE branch_id=? "
                + "ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END, full_name";
        List<Member> list = new ArrayList<>();
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

    /** ACTIVE members for one branch - the population dues/expiry are computed against. */
    public List<Member> findActiveByBranch(int branchId) throws SQLException {
        String sql = "SELECT * FROM members WHERE branch_id=? AND status='ACTIVE' ORDER BY full_name";
        List<Member> list = new ArrayList<>();
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

    public Member findById(int memberId) throws SQLException {
        String sql = "SELECT * FROM members WHERE member_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Member m) throws SQLException {
        String sql = "INSERT INTO members (branch_id, full_name, phone, email, gender, dob, address, "
                + "photo_path, join_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getBranchId());
            ps.setString(2, m.getFullName());
            ps.setString(3, m.getPhone());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getGender());
            ps.setDate(6, m.getDob() == null ? null : Date.valueOf(m.getDob()));
            ps.setString(7, m.getAddress());
            ps.setString(8, m.getPhotoPath());
            ps.setDate(9, m.getJoinDate() == null ? null : Date.valueOf(m.getJoinDate()));
            ps.setString(10, m.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert failed, no generated key returned for members");
    }

    public void update(Member m) throws SQLException {
        String sql = "UPDATE members SET full_name=?, phone=?, email=?, gender=?, dob=?, address=?, "
                + "join_date=?, status=? WHERE member_id=? AND branch_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getFullName());
            ps.setString(2, m.getPhone());
            ps.setString(3, m.getEmail());
            ps.setString(4, m.getGender());
            ps.setDate(5, m.getDob() == null ? null : Date.valueOf(m.getDob()));
            ps.setString(6, m.getAddress());
            ps.setDate(7, m.getJoinDate() == null ? null : Date.valueOf(m.getJoinDate()));
            ps.setString(8, m.getStatus());
            ps.setInt(9, m.getMemberId());
            ps.setInt(10, m.getBranchId());
            ps.executeUpdate();
        }
    }

    public void delete(int branchId, int memberId) throws SQLException {
        String sql = "DELETE FROM members WHERE member_id=? AND branch_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    private Member map(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setBranchId(rs.getInt("branch_id"));
        m.setFullName(rs.getString("full_name"));
        m.setPhone(rs.getString("phone"));
        m.setEmail(rs.getString("email"));
        m.setGender(rs.getString("gender"));
        java.sql.Date dob = rs.getDate("dob");
        m.setDob(dob == null ? null : dob.toLocalDate());
        m.setAddress(rs.getString("address"));
        m.setPhotoPath(rs.getString("photo_path"));
        java.sql.Date join = rs.getDate("join_date");
        m.setJoinDate(join == null ? null : join.toLocalDate());
        m.setStatus(rs.getString("status"));
        return m;
    }
}
