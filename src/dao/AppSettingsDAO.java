package dao;

import db.DBConnection;
import model.AppSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Reads/writes the single (id=1) app_settings row - business-wide info shown on the Settings screen. */
public class AppSettingsDAO {

    public AppSettings find() throws SQLException {
        String sql = "SELECT * FROM app_settings WHERE id=1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            AppSettings s = new AppSettings();
            if (rs.next()) {
                s.setBusinessName(rs.getString("business_name"));
                s.setAddress(rs.getString("address"));
                s.setPhone(rs.getString("phone"));
                s.setEmail(rs.getString("email"));
                s.setCurrencySymbol(rs.getString("currency_symbol"));
                s.setReminderDaysBeforeExpiry(rs.getInt("reminder_days_before_expiry"));
                s.setSmsEnabled(rs.getBoolean("sms_enabled"));
            }
            return s;
        }
    }

    public void update(AppSettings s) throws SQLException {
        String sql = "UPDATE app_settings SET business_name=?, address=?, phone=?, email=?, currency_symbol=?, "
                + "reminder_days_before_expiry=?, sms_enabled=?, updated_at=GETDATE() WHERE id=1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getBusinessName());
            ps.setString(2, s.getAddress());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getCurrencySymbol());
            ps.setInt(6, s.getReminderDaysBeforeExpiry());
            ps.setBoolean(7, s.isSmsEnabled());
            ps.executeUpdate();
        }
    }
}
