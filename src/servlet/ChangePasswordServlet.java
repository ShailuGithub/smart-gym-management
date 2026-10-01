package servlet;

import dao.UserDAO;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/Forms/Security/change_password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            if (!userDAO.verifyPassword(user.getUserId(), currentPassword)) {
                request.setAttribute("error", "Current password is incorrect.");
                request.getRequestDispatcher("/Forms/Security/change_password.jsp").forward(request, response);
                return;
            }
            if (newPassword == null || newPassword.length() < 6) {
                request.setAttribute("error", "New password must be at least 6 characters.");
                request.getRequestDispatcher("/Forms/Security/change_password.jsp").forward(request, response);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                request.setAttribute("error", "New password and confirmation do not match.");
                request.getRequestDispatcher("/Forms/Security/change_password.jsp").forward(request, response);
                return;
            }
            userDAO.updatePassword(user.getUserId(), newPassword);
            response.sendRedirect(request.getContextPath() + "/change-password?saved=1");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
