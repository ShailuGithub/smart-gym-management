package api;

import dao.AppSettingsDAO;
import model.AppSettings;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /api/settings - business-wide settings (any authenticated user can read them, e.g. to show
 * the currency symbol or reminder threshold in the mobile UI).
 * PUT /api/settings - update them; admin-only, mirroring servlet.SettingsServlet's own
 * admin-only rule for the web Settings screen.
 */
@WebServlet("/api/settings")
public class SettingsApiServlet extends HttpServlet {

    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    private static final class SettingsInput {
        String businessName;
        String address;
        String phone;
        String email;
        String currencySymbol;
        Integer reminderDaysBeforeExpiry;
        Boolean smsEnabled;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        try {
            JsonUtil.writeJson(response, appSettingsDAO.find());
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.isAdmin()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "Only an Admin can change Settings.");
            return;
        }
        SettingsInput input;
        try {
            input = JsonUtil.readJson(request, SettingsInput.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        if (input == null) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Request body required.");
            return;
        }
        try {
            AppSettings s = appSettingsDAO.find();
            if (input.businessName != null) { s.setBusinessName(input.businessName); }
            if (input.address != null) { s.setAddress(input.address); }
            if (input.phone != null) { s.setPhone(input.phone); }
            if (input.email != null) { s.setEmail(input.email); }
            if (input.currencySymbol != null) { s.setCurrencySymbol(input.currencySymbol); }
            if (input.reminderDaysBeforeExpiry != null) { s.setReminderDaysBeforeExpiry(input.reminderDaysBeforeExpiry); }
            if (input.smsEnabled != null) { s.setSmsEnabled(input.smsEnabled); }
            appSettingsDAO.update(s);
            ApiAudit.record(user, null, "UPDATE", "SETTINGS", null, "Updated app settings");
            JsonUtil.writeJson(response, appSettingsDAO.find());
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
