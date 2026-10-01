package servlet;

import dao.GymBranchDAO;
import model.GymBranch;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** "Branches" screen - ADMIN-only. Lets the gym owner add/edit/deactivate branches/locations. */
@WebServlet("/branches")
public class BranchServlet extends HttpServlet {

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
                String branchIdParam = request.getParameter("branchId");
                if (branchIdParam != null) {
                    request.setAttribute("branch", gymBranchDAO.findById(Integer.parseInt(branchIdParam)));
                }
                request.getRequestDispatcher("/Forms/Masters/branch_form.jsp").forward(request, response);
                return;
            }
            request.setAttribute("branches", gymBranchDAO.findAll());
            request.getRequestDispatcher("/Forms/Masters/branches.jsp").forward(request, response);
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
        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "add": {
                    GymBranch b = readForm(request);
                    int newId = gymBranchDAO.insert(b);
                    AuditLog.record(request, newId, "CREATE", "BRANCH", newId, "Created branch: " + b.getName());
                    break;
                }
                case "update": {
                    GymBranch b = readForm(request);
                    b.setBranchId(Integer.parseInt(request.getParameter("branchId")));
                    gymBranchDAO.update(b);
                    AuditLog.record(request, b.getBranchId(), "UPDATE", "BRANCH", b.getBranchId(), "Updated branch: " + b.getName());
                    break;
                }
                case "delete": {
                    int branchId = Integer.parseInt(request.getParameter("branchId"));
                    GymBranch existing = gymBranchDAO.findById(branchId);
                    gymBranchDAO.delete(branchId);
                    AuditLog.record(request, branchId, "DELETE", "BRANCH", branchId,
                            "Deleted branch: " + (existing != null ? existing.getName() : "#" + branchId));
                    break;
                }
                default:
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        response.sendRedirect(request.getContextPath() + "/branches");
    }

    private GymBranch readForm(HttpServletRequest request) {
        GymBranch b = new GymBranch();
        b.setName(request.getParameter("name"));
        b.setAddress(request.getParameter("address"));
        b.setCity(request.getParameter("city"));
        b.setState(request.getParameter("state"));
        b.setPhone(request.getParameter("phone"));
        b.setEmail(request.getParameter("email"));
        String status = request.getParameter("status");
        b.setStatus(GymBranch.INACTIVE.equals(status) ? GymBranch.INACTIVE : GymBranch.ACTIVE);
        return b;
    }
}
