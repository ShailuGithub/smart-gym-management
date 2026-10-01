package servlet;

import dao.UserDAO;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // Deters credential-stuffing/brute-force scripts hammering the login endpoint from one
    // address - see RateLimiter's own doc comment for what this does and doesn't cover.
    private static final RateLimiter RATE_LIMITER = RateLimiter.forName("login", 15, 15 * 60 * 1000L);

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/Forms/Security/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!RATE_LIMITER.allow(request.getRemoteAddr())) {
            request.setAttribute("error", "Too many login attempts from your network. Please wait a few minutes and try again.");
            request.getRequestDispatcher("/Forms/Security/login.jsp").forward(request, response);
            return;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        try {
            User user;
            try {
                user = userDAO.authenticate(username, password);
            } catch (UserDAO.AccountLockedException e) {
                request.setAttribute("error", "Too many failed login attempts. This account is locked for about "
                        + e.getMinutesRemaining() + " more minute(s) - or reset your password to unlock it immediately.");
                request.getRequestDispatcher("/Forms/Security/login.jsp").forward(request, response);
                return;
            }
            if (user == null) {
                request.setAttribute("error", "Invalid username or password.");
                request.getRequestDispatcher("/Forms/Security/login.jsp").forward(request, response);
                return;
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp");
        } catch (SQLException e) {
            request.setAttribute("error", "Database error: " + e.getMessage());
            request.getRequestDispatcher("/Forms/Security/login.jsp").forward(request, response);
        }
    }
}
