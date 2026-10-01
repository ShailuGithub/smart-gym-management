package servlet;

import dao.MembershipPlanDAO;
import model.MembershipPlan;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * "Membership Plans" screen - business-wide (not per-branch), so unlike Members/Branches there's
 * no branch-scope check here. Viewing/editing is open to Admin+Manager (canAccessPlans()); actual
 * deletion is Admin-only since removing a plan gym-wide is a more destructive, cross-branch action
 * than editing its price.
 */
@WebServlet("/plans")
public class PlanServlet extends HttpServlet {

    private final MembershipPlanDAO planDAO = new MembershipPlanDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessPlans()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            if ("form".equals(request.getParameter("view"))) {
                String planIdParam = request.getParameter("planId");
                if (planIdParam != null) {
                    request.setAttribute("plan", planDAO.findById(Integer.parseInt(planIdParam)));
                }
                request.getRequestDispatcher("/Forms/Masters/plan_form.jsp").forward(request, response);
                return;
            }
            request.setAttribute("plans", planDAO.findAll());
            request.getRequestDispatcher("/Forms/Masters/plans.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessPlans()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "add": {
                    if (!user.canCreate()) {
                        Permissions.deny(request, response, "/plans");
                        return;
                    }
                    MembershipPlan p = readForm(request);
                    int newId = planDAO.insert(p);
                    AuditLog.record(request, null, "CREATE", "PLAN", newId, "Created plan: " + p.getName());
                    break;
                }
                case "update": {
                    if (!user.canEdit()) {
                        Permissions.deny(request, response, "/plans");
                        return;
                    }
                    MembershipPlan p = readForm(request);
                    p.setPlanId(Integer.parseInt(request.getParameter("planId")));
                    planDAO.update(p);
                    AuditLog.record(request, null, "UPDATE", "PLAN", p.getPlanId(), "Updated plan: " + p.getName());
                    break;
                }
                case "delete": {
                    if (!user.isAdmin()) {
                        Permissions.deny(request, response, "/plans");
                        return;
                    }
                    int planId = Integer.parseInt(request.getParameter("planId"));
                    MembershipPlan existing = planDAO.findById(planId);
                    planDAO.delete(planId);
                    AuditLog.record(request, null, "DELETE", "PLAN", planId,
                            "Deleted plan: " + (existing != null ? existing.getName() : "#" + planId));
                    break;
                }
                default:
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        response.sendRedirect(request.getContextPath() + "/plans");
    }

    private MembershipPlan readForm(HttpServletRequest request) {
        MembershipPlan p = new MembershipPlan();
        p.setName(request.getParameter("name"));
        String duration = request.getParameter("durationDays");
        p.setDurationDays(duration == null || duration.trim().isEmpty() ? 30 : Integer.parseInt(duration));
        String price = request.getParameter("price");
        p.setPrice(price == null || price.trim().isEmpty() ? BigDecimal.ZERO : new BigDecimal(price));
        p.setDescription(request.getParameter("description"));
        String status = request.getParameter("status");
        p.setStatus(MembershipPlan.INACTIVE.equals(status) ? MembershipPlan.INACTIVE : MembershipPlan.ACTIVE);
        return p;
    }
}
