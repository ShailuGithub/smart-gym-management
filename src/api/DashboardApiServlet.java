package api;

import dao.AppSettingsDAO;
import dao.GymBranchDAO;
import dao.MemberDAO;
import dao.MemberSubscriptionDAO;
import dao.PaymentDAO;
import dao.UserDAO;
import model.AppSettings;
import model.GymBranch;
import model.Member;
import model.MemberSubscription;
import model.MembershipStatus;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * GET /api/dashboard[?branchId=] - the same stats web/dashboard.jsp shows, aggregated over
 * whichever branches are in scope for the authenticated user (see ApiSupport.resolveBranchScope):
 * every branch for an ADMIN (optionally narrowed with ?branchId=), or just their own for a
 * MANAGER/STAFF.
 */
@WebServlet("/api/dashboard")
public class DashboardApiServlet extends HttpServlet {

    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();
    private final UserDAO userDAO = new UserDAO();

    private static final class DashboardStats {
        int activeMembers;
        int expiringSoon;
        int expiredOrNoPlan;
        BigDecimal todaysCollection = BigDecimal.ZERO;
        int branchCount;
        int userCount;
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
            List<Integer> branchScope = ApiSupport.resolveBranchScope(user, gymBranchDAO, request.getParameter("branchId"));
            AppSettings settings = appSettingsDAO.find();

            DashboardStats stats = new DashboardStats();
            LocalDate today = LocalDate.now();
            for (int branchId : branchScope) {
                List<Member> activeMembers = memberDAO.findActiveByBranch(branchId);
                Map<Integer, MemberSubscription> latestByMember = subscriptionDAO.findLatestForBranch(branchId);
                stats.activeMembers += activeMembers.size();
                for (Member m : activeMembers) {
                    MembershipStatus status = MembershipStatus.of(m.getMemberId(), latestByMember.get(m.getMemberId()),
                            settings.getReminderDaysBeforeExpiry());
                    if (status.isExpiringSoon()) {
                        stats.expiringSoon++;
                    } else if (status.isExpired() || status.hasNoPlan()) {
                        stats.expiredOrNoPlan++;
                    }
                }
                stats.todaysCollection = stats.todaysCollection.add(paymentDAO.totalForBranchAndDate(branchId, today));
            }

            if (user.isAdmin()) {
                List<GymBranch> allBranches = gymBranchDAO.findAll();
                stats.branchCount = allBranches.size();
                stats.userCount = userDAO.findAll().size();
            }

            JsonUtil.writeJson(response, stats);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
