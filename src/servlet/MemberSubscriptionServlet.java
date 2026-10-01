package servlet;

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
 * Assigns a membership plan to a member, or renews their current one. The actual date math and
 * payment-creation logic lives in service.SubscriptionService (shared with api.SubscriptionApiServlet)
 * - this servlet is just the web request/response/permission/audit-log wiring around it.
 */
@WebServlet("/subscriptions")
public class MemberSubscriptionServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();
    private final MembershipPlanDAO planDAO = new MembershipPlanDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessMembers()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            int memberId = Integer.parseInt(request.getParameter("memberId"));
            Member member = memberDAO.findById(memberId);
            if (member == null || !MemberServlet.canAccess(user, member.getBranchId())) {
                Permissions.deny(request, response, "/members");
                return;
            }
            if (!user.canEdit()) {
                Permissions.deny(request, response, "/members?memberId=" + memberId);
                return;
            }

            int planId = Integer.parseInt(request.getParameter("planId"));
            MembershipPlan plan = planDAO.findById(planId);
            if (plan == null) {
                Permissions.deny(request, response, "/members?memberId=" + memberId);
                return;
            }

            String startParam = request.getParameter("startDate");
            LocalDate startDateOverride = startParam == null || startParam.trim().isEmpty()
                    ? null : LocalDate.parse(startParam);

            String amountParam = request.getParameter("amount");
            BigDecimal amountOverride = amountParam == null || amountParam.trim().isEmpty()
                    ? null : new BigDecimal(amountParam);

            String mode = request.getParameter("mode");

            SubscriptionService.Result result = SubscriptionService.assignOrRenew(
                    member, plan, startDateOverride, amountOverride, mode);

            AuditLog.record(request, member.getBranchId(), "CREATE", "SUBSCRIPTION", memberId,
                    "Assigned plan '" + plan.getName() + "' to " + member.getFullName()
                            + " (valid " + result.subscription.getStartDate() + " to " + result.subscription.getEndDate() + ")");
            AuditLog.record(request, member.getBranchId(), "CREATE", "PAYMENT", result.payment.getPaymentId(),
                    "Recorded payment of Rs. " + result.payment.getAmount() + " from " + member.getFullName()
                            + " for plan '" + plan.getName() + "'");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        response.sendRedirect(request.getContextPath() + "/members?memberId=" + request.getParameter("memberId"));
    }
}
