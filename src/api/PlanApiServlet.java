package api;

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
 * Membership Plans - business-wide (no branch scope), mirroring servlet.PlanServlet's rules:
 * viewing/editing is open to Admin+Manager (canAccessPlans()), deletion is Admin-only since
 * removing a plan gym-wide is a more destructive, cross-branch action than editing its price.
 *
 * GET /api/plans        -> active plans only (any authenticated user - e.g. the assign/renew picker)
 * GET /api/plans/{id}   -> one plan (canAccessPlans())
 * POST /api/plans       -> create (canAccessPlans() + canCreate())
 * PUT /api/plans/{id}   -> update (canAccessPlans() + canEdit())
 * DELETE /api/plans/{id} -> delete (admin-only)
 */
@WebServlet("/api/plans/*")
public class PlanApiServlet extends HttpServlet {

    private final MembershipPlanDAO planDAO = new MembershipPlanDAO();

    private static final class PlanInput {
        String name;
        Integer durationDays;
        BigDecimal price;
        String description;
        String status;
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
            Integer planId = pathId(request);
            if (planId != null) {
                if (!user.canAccessPlans()) {
                    JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to Membership Plans.");
                    return;
                }
                MembershipPlan plan = planDAO.findById(planId);
                if (plan == null) {
                    JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Plan not found.");
                    return;
                }
                JsonUtil.writeJson(response, plan);
                return;
            }
            // Collection root: active plans only by default (the assign/renew picker needs this
            // even for a STAFF account that can't manage plans themselves) - pass ?all=true (only
            // honored for someone who can actually manage plans) to also see INACTIVE ones, same
            // as the web Plans screen shows every status.
            boolean wantAll = "true".equals(request.getParameter("all"));
            if (wantAll && !user.canAccessPlans()) {
                JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to Membership Plans.");
                return;
            }
            JsonUtil.writeJson(response, wantAll ? planDAO.findAll() : planDAO.findAllActive());
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessPlans() || !user.canCreate()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have permission to add plans.");
            return;
        }
        PlanInput input;
        try {
            input = JsonUtil.readJson(request, PlanInput.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        if (input == null || input.name == null || input.name.trim().isEmpty()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "name is required.");
            return;
        }
        try {
            MembershipPlan p = fromInput(input, null);
            int newId = planDAO.insert(p);
            ApiAudit.record(user, null, "CREATE", "PLAN", newId, "Created plan: " + p.getName());
            JsonUtil.writeJson(response, HttpServletResponse.SC_CREATED, planDAO.findById(newId));
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
        if (!user.canAccessPlans() || !user.canEdit()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have permission to edit plans.");
            return;
        }
        Integer planId = pathId(request);
        if (planId == null) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "PUT /api/plans/{id} requires an id.");
            return;
        }
        try {
            MembershipPlan existing = planDAO.findById(planId);
            if (existing == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Plan not found.");
                return;
            }
            PlanInput input;
            try {
                input = JsonUtil.readJson(request, PlanInput.class);
            } catch (Exception e) {
                JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
                return;
            }
            MembershipPlan p = fromInput(input, existing);
            p.setPlanId(planId);
            planDAO.update(p);
            ApiAudit.record(user, null, "UPDATE", "PLAN", planId, "Updated plan: " + p.getName());
            JsonUtil.writeJson(response, planDAO.findById(planId));
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.isAdmin()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "Only an Admin can delete plans.");
            return;
        }
        Integer planId = pathId(request);
        if (planId == null) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "DELETE /api/plans/{id} requires an id.");
            return;
        }
        try {
            MembershipPlan existing = planDAO.findById(planId);
            if (existing == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Plan not found.");
                return;
            }
            planDAO.delete(planId);
            ApiAudit.record(user, null, "DELETE", "PLAN", planId, "Deleted plan: " + existing.getName());
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    /** Builds a MembershipPlan from the input, falling back to existing's values for any field left null (PUT semantics). */
    private MembershipPlan fromInput(PlanInput input, MembershipPlan existing) {
        MembershipPlan p = new MembershipPlan();
        p.setName(input != null && input.name != null ? input.name : (existing != null ? existing.getName() : null));
        p.setDurationDays(input != null && input.durationDays != null ? input.durationDays
                : (existing != null ? existing.getDurationDays() : 30));
        p.setPrice(input != null && input.price != null ? input.price
                : (existing != null ? existing.getPrice() : BigDecimal.ZERO));
        p.setDescription(input != null && input.description != null ? input.description
                : (existing != null ? existing.getDescription() : null));
        String status = input != null ? input.status : null;
        p.setStatus(MembershipPlan.INACTIVE.equals(status) ? MembershipPlan.INACTIVE
                : (status == null && existing != null ? existing.getStatus() : MembershipPlan.ACTIVE));
        return p;
    }

    private Integer pathId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            return null;
        }
        String segment = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
