package servlet;

import dao.AuditLogDAO;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** "Audit Log" screen - ADMIN-only, most recent 200 create/edit/delete actions, newest first.
 *  Uses the same branch-switcher scoping as every other screen: "All Branches" shows everything. */
@WebServlet("/audit-log")
public class AuditLogServlet extends HttpServlet {

    private static final int MAX_ENTRIES = 200;

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.isAdmin()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            Integer branchId = BranchContext.currentBranchIdOrNull(request);
            request.setAttribute("entries", branchId == null
                    ? auditLogDAO.findAll(MAX_ENTRIES) : auditLogDAO.findByBranch(branchId, MAX_ENTRIES));
            request.getRequestDispatcher("/Forms/Security/audit_log.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
