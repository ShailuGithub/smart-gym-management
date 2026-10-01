package api;

import dao.MemberDAO;
import dao.MembershipPlanDAO;
import model.Member;
import model.MembershipPlan;
import model.User;
import service.SubscriptionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * POST /api/subscriptions - assign a membership plan to a member, or renew their current one.
 * Delegates the actual date math and payment-creation to service.SubscriptionService, the same
 * shared logic servlet.MemberSubscriptionServlet uses, so the two never drift apart.
 */
@WebServlet("/api/subscriptions")
public class SubscriptionApiServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();
    private final MembershipPlanDAO planDAO = new MembershipPlanDAO();

    private static final class SubscriptionInput {
        int memberId;
        int planId;
        LocalDate startDate; // optional override
        BigDecimal amount;   // optional override, defaults to the plan's price
        String mode;         // optional, defaults to CASH
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessMembers() || !user.canEdit()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have permission to assign/renew plans.");
            return;
        }
        SubscriptionInput input;
        try {
            input = JsonUtil.readJson(request, SubscriptionInput.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        if (input == null || input.memberId <= 0 || input.planId <= 0) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "memberId and planId are required.");
            return;
        }
        try {
            Member member = memberDAO.findById(input.memberId);
            if (member == null || !ApiSupport.canAccessBranch(user, member.getBranchId())) {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Member not found.");
                return;
            }
            MembershipPlan plan = planDAO.findById(input.planId);
            if (plan == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Plan not found.");
                return;
            }

            SubscriptionService.Result result = SubscriptionService.assignOrRenew(
                    member, plan, input.startDate, input.amount, input.mode);

            ApiAudit.record(user, member.getBranchId(), "CREATE", "SUBSCRIPTION", member.getMemberId(),
                    "Assigned plan '" + plan.getName() + "' to " + member.getFullName()
                            + " (valid " + result.subscription.getStartDate() + " to " + result.subscription.getEndDate() + ")");
            ApiAudit.record(user, member.getBranchId(), "CREATE", "PAYMENT", result.payment.getPaymentId(),
                    "Recorded payment of Rs. " + result.payment.getAmount() + " from " + member.getFullName()
                            + " for plan '" + plan.getName() + "'");

            JsonUtil.writeJson(response, result);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
