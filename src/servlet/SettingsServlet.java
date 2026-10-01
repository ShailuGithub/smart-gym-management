package servlet;

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

/** "Settings" screen - ADMIN-only, business-wide info (not tied to any one branch). */
@WebServlet("/settings")
public class SettingsServlet extends HttpServlet {

    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.isAdmin()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            request.setAttribute("settings", appSettingsDAO.find());
            request.getRequestDispatcher("/Forms/Utilities/settings.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.isAdmin()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            AppSettings s = new AppSettings();
            s.setBusinessName(request.getParameter("businessName"));
            s.setAddress(request.getParameter("address"));
            s.setPhone(request.getParameter("phone"));
            s.setEmail(request.getParameter("email"));
            String currency = request.getParameter("currencySymbol");
            s.setCurrencySymbol(currency == null || currency.trim().isEmpty() ? "Rs." : currency.trim());
            s.setReminderDaysBeforeExpiry(clamp(request.getParameter("reminderDaysBeforeExpiry"), 3, 0, 30));
            s.setSmsEnabled("on".equals(request.getParameter("smsEnabled")));
            appSettingsDAO.update(s);
            AuditLog.record(request, null, "UPDATE", "SETTINGS", null, "Updated business settings");
            response.sendRedirect(request.getContextPath() + "/settings?saved=1");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Parses an int form field, falling back to fallback on blank/invalid input, clamped to [min, max]. */
    private int clamp(String param, int fallback, int min, int max) {
        int value = fallback;
        if (param != null && !param.trim().isEmpty()) {
            try {
                value = Integer.parseInt(param.trim());
            } catch (NumberFormatException ignored) {
                value = fallback;
            }
        }
        return Math.max(min, Math.min(max, value));
    }
}
