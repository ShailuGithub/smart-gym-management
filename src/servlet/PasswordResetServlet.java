package servlet;

import dao.PasswordResetDAO;
import dao.UserDAO;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Self-service password reset, public (pre-login) - see LoginFilter's isPublic check for the
 * exemption. Always shows the same generic "if that matches our records..." message on request,
 * regardless of whether the username/email actually matched anything, so this can't be used to
 * enumerate valid usernames or emails.
 *
 * No SMTP/email service is wired up in this project (kept dependency-free) - the reset link is
 * shown directly on the confirmation screen instead of being emailed. That's a stand-in for real
 * delivery: swap issueToken()'s "show the link" step for an actual mail send when one is needed.
 */
@WebServlet({"/forgot-password", "/reset-password"})
public class PasswordResetServlet extends HttpServlet {

    private static final int TOKEN_VALID_HOURS = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Deters bots hammering this endpoint to enumerate accounts - see RateLimiter's own doc
    // comment for what this does and doesn't cover.
    private static final RateLimiter RATE_LIMITER = RateLimiter.forName("forgot-password", 5, 60 * 60 * 1000L);

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/reset-password".equals(request.getServletPath())) {
            String token = request.getParameter("token");
            try {
                boolean valid = token != null && passwordResetDAO.findUserIdForValidToken(token) != null;
                request.setAttribute("valid", valid);
                request.setAttribute("token", token);
            } catch (SQLException e) {
                throw new ServletException(e);
            }
            request.getRequestDispatcher("/Forms/Security/reset_password.jsp").forward(request, response);
            return;
        }
        request.getRequestDispatcher("/Forms/Security/forgot_password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/reset-password".equals(request.getServletPath())) {
            handleResetPassword(request, response);
        } else {
            handleForgotPassword(request, response);
        }
    }

    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        // Honeypot: a real browser never fills this in (it's hidden off-screen in
        // forgot_password.jsp). Treat it exactly like a rate-limit hit below - same generic
        // response, no token issued.
        String honeypot = request.getParameter("website");
        boolean isBot = (honeypot != null && !honeypot.trim().isEmpty()) || !RATE_LIMITER.allow(request.getRemoteAddr());

        try {
            User user = isBot || username == null ? null : userDAO.findByUsername(username.trim());
            String resetLink = null;
            if (user != null && user.getEmail() != null
                    && user.getEmail().trim().equalsIgnoreCase(email == null ? "" : email.trim())) {
                resetLink = issueToken(request, user);
            }
            // Same message either way - don't reveal whether the username/email matched
            // anything, or whether this request even made it past the honeypot/rate limit.
            // The link itself is only ever set (and shown) when there was a real match.
            request.setAttribute("submitted", Boolean.TRUE);
            request.setAttribute("resetLink", resetLink);
            request.getRequestDispatcher("/Forms/Security/forgot_password.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private String issueToken(HttpServletRequest request, User user) throws SQLException {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        passwordResetDAO.createToken(user.getUserId(), token, LocalDateTime.now().plusHours(TOKEN_VALID_HOURS));

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (isDefaultPort(request) ? "" : ":" + request.getServerPort())
                + request.getContextPath();
        return baseUrl + "/reset-password?token=" + token;
    }

    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            Integer userId = token == null ? null : passwordResetDAO.findUserIdForValidToken(token);
            if (userId == null) {
                request.setAttribute("valid", Boolean.FALSE);
                request.getRequestDispatcher("/Forms/Security/reset_password.jsp").forward(request, response);
                return;
            }
            if (newPassword == null || newPassword.length() < 6 || !newPassword.equals(confirmPassword)) {
                request.setAttribute("valid", Boolean.TRUE);
                request.setAttribute("token", token);
                request.setAttribute("error", "Passwords must match and be at least 6 characters.");
                request.getRequestDispatcher("/Forms/Security/reset_password.jsp").forward(request, response);
                return;
            }

            userDAO.updatePassword(userId, newPassword);
            // Also lifts any login lockout - a completed reset is a legitimate way back in,
            // not something that should still be waiting out a timer afterward.
            userDAO.resetFailedAttempts(userId);
            passwordResetDAO.markUsed(token);
            response.sendRedirect(request.getContextPath() + "/login?reset=1");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private boolean isDefaultPort(HttpServletRequest request) {
        int port = request.getServerPort();
        return ("http".equals(request.getScheme()) && port == 80) || ("https".equals(request.getScheme()) && port == 443);
    }
}
