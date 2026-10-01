package servlet;

import dao.GymBranchDAO;
import dao.UserDAO;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * "Manage Users" screen - ADMIN-only, since it's how permissions themselves get changed.
 * Creates/edits accounts of all 3 roles (Owner/Admin, Manager, Staff); any non-ADMIN role is
 * always assigned to exactly one branch with per-user Create/Edit/Delete flags, an ADMIN
 * always has full rights everywhere and isn't tied to any single branch.
 */
@WebServlet("/users")
public class UserManagementServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.isAdmin()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            if ("form".equals(request.getParameter("view"))) {
                request.setAttribute("branches", gymBranchDAO.findAll());
                request.getRequestDispatcher("/Forms/Security/user_form.jsp").forward(request, response);
                return;
            }
            request.setAttribute("users", userDAO.findAll());
            request.setAttribute("branches", gymBranchDAO.findAll());
            request.getRequestDispatcher("/Forms/Security/users.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = Permissions.userOf(request);
        if (!currentUser.isAdmin()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }

        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "create": {
                    String username = request.getParameter("username");
                    String password = request.getParameter("password");
                    String fullName = request.getParameter("fullName");
                    String email = request.getParameter("email");
                    String roleParam = request.getParameter("role");
                    String role = isValidRole(roleParam) ? roleParam : User.MANAGER;
                    if (userDAO.usernameExists(username)) {
                        request.setAttribute("branches", gymBranchDAO.findAll());
                        request.setAttribute("error", "That username is already taken.");
                        request.getRequestDispatcher("/Forms/Security/user_form.jsp").forward(request, response);
                        return;
                    }
                    Integer branchId = null;
                    boolean canCreate = true, canEdit = true, canDelete = false;
                    if (!User.ADMIN.equals(role)) {
                        String branchIdParam = request.getParameter("branchId");
                        if (branchIdParam == null || branchIdParam.trim().isEmpty()) {
                            request.setAttribute("branches", gymBranchDAO.findAll());
                            request.setAttribute("error", "Add a branch first before creating this account.");
                            request.getRequestDispatcher("/Forms/Security/user_form.jsp").forward(request, response);
                            return;
                        }
                        branchId = Integer.parseInt(branchIdParam);
                        canCreate = "on".equals(request.getParameter("canCreate"));
                        canEdit = "on".equals(request.getParameter("canEdit"));
                        canDelete = "on".equals(request.getParameter("canDelete"));
                    }
                    int newUserId = userDAO.insert(username, password, fullName, email, role, branchId,
                            canCreate, canEdit, canDelete);
                    AuditLog.record(request, null, "CREATE", "USER", newUserId, "Created " + role + " user: " + username);
                    break;
                }
                case "update": {
                    int userId = Integer.parseInt(request.getParameter("userId"));
                    String fullName = request.getParameter("fullName");
                    String email = request.getParameter("email");
                    User target = userDAO.findById(userId);
                    Integer branchId = target.getBranchId();
                    boolean canCreate = target.isPermCreate(), canEdit = target.isPermEdit(), canDelete = target.isPermDelete();
                    if (!target.isAdmin()) {
                        branchId = Integer.parseInt(request.getParameter("branchId"));
                        canCreate = "on".equals(request.getParameter("canCreate"));
                        canEdit = "on".equals(request.getParameter("canEdit"));
                        canDelete = "on".equals(request.getParameter("canDelete"));
                    }
                    userDAO.update(userId, fullName, email, branchId, canCreate, canEdit, canDelete);
                    AuditLog.record(request, branchId, "UPDATE", "USER", userId, "Updated user: " + target.getUsername());
                    break;
                }
                case "delete": {
                    int userId = Integer.parseInt(request.getParameter("userId"));
                    if (userId == currentUser.getUserId()) {
                        response.sendRedirect(request.getContextPath() + "/users?error=selfDelete");
                        return;
                    }
                    User target = userDAO.findById(userId);
                    if (target != null && target.isAdmin() && countAdmins() <= 1) {
                        response.sendRedirect(request.getContextPath() + "/users?error=lastAdmin");
                        return;
                    }
                    userDAO.delete(userId);
                    AuditLog.record(request, target != null ? target.getBranchId() : null, "DELETE", "USER", userId,
                            "Deleted user: " + (target != null ? target.getUsername() : "#" + userId));
                    break;
                }
                default:
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        response.sendRedirect(request.getContextPath() + "/users");
    }

    private boolean isValidRole(String role) {
        return User.ADMIN.equals(role) || User.MANAGER.equals(role) || User.STAFF.equals(role);
    }

    private int countAdmins() throws SQLException {
        int count = 0;
        List<User> all = userDAO.findAll();
        for (User u : all) {
            if (u.isAdmin()) {
                count++;
            }
        }
        return count;
    }
}
