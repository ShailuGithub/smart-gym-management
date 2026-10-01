package servlet;

import dao.GymBranchDAO;
import dao.MemberDAO;
import dao.MemberSubscriptionDAO;
import dao.MembershipPlanDAO;
import dao.AppSettingsDAO;
import dao.PaymentDAO;
import model.AppSettings;
import model.GymBranch;
import model.Member;
import model.MemberSubscription;
import model.MembershipStatus;
import model.Payment;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Member CRUD for whichever single branch is currently in scope (see BranchContext) - a member
 * always belongs to exactly one branch, so this never silently guesses which one: an ADMIN
 * viewing "All Branches" must pick a specific branch via the switcher before adding a member.
 * Unlike the reference project's Student screen, there's no separate Vacate/Rejoin flow - a
 * member's ACTIVE/INACTIVE status is just an editable field here.
 */
@WebServlet("/members")
public class MemberServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();
    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();
    private final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private final MembershipPlanDAO planDAO = new MembershipPlanDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessMembers()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            List<GymBranch> allBranches = gymBranchDAO.findAll();
            if (allBranches.isEmpty()) {
                request.setAttribute("noBranch", true);
                request.getRequestDispatcher("/Forms/Transaction/members.jsp").forward(request, response);
                return;
            }

            String memberIdParam = request.getParameter("memberId");
            String view = request.getParameter("view");

            if ("form".equals(view) && memberIdParam == null) {
                // ADD: must have exactly one concrete branch in scope - never silently guess.
                Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
                if (scopedBranchId == null) {
                    request.setAttribute("selectBranchFirst", true);
                    request.setAttribute("allBranches", allBranches);
                    request.getRequestDispatcher("/Forms/Transaction/members.jsp").forward(request, response);
                    return;
                }
                request.setAttribute("targetBranchName", branchName(allBranches, scopedBranchId));
                request.getRequestDispatcher("/Forms/Transaction/member_form.jsp").forward(request, response);
                return;
            }

            if (memberIdParam != null) {
                // Edit/view always act on the member's own true branch (never the ambient
                // header-switcher selection, which a MANAGER must never be trusted to bypass).
                Member member = memberDAO.findById(Integer.parseInt(memberIdParam));
                if (member == null || !canAccess(user, member.getBranchId())) {
                    Permissions.deny(request, response, "/members");
                    return;
                }
                if ("form".equals(view)) {
                    request.setAttribute("member", member);
                    request.setAttribute("targetBranchName", branchName(allBranches, member.getBranchId()));
                    request.getRequestDispatcher("/Forms/Transaction/member_form.jsp").forward(request, response);
                    return;
                }
                AppSettings settings = appSettingsDAO.find();
                List<MemberSubscription> history = subscriptionDAO.findByMember(member.getMemberId());
                MemberSubscription latest = history.isEmpty() ? null : subscriptionDAO.findLatestForMember(member.getMemberId());
                request.setAttribute("member", member);
                request.setAttribute("targetBranchName", branchName(allBranches, member.getBranchId()));
                request.setAttribute("subscriptionHistory", history);
                request.setAttribute("membershipStatus",
                        MembershipStatus.of(member.getMemberId(), latest, settings.getReminderDaysBeforeExpiry()));
                request.setAttribute("activePlans", planDAO.findAllActive());
                request.setAttribute("paymentHistory", paymentDAO.findByMember(member.getMemberId()));
                request.getRequestDispatcher("/Forms/Transaction/member_view.jsp").forward(request, response);
                return;
            }

            // List view - same "never silently guess" rule as ADD.
            Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
            if (scopedBranchId == null) {
                request.setAttribute("selectBranchFirst", true);
                request.setAttribute("allBranches", allBranches);
                request.getRequestDispatcher("/Forms/Transaction/members.jsp").forward(request, response);
                return;
            }
            List<Member> members = memberDAO.findByBranch(scopedBranchId);
            AppSettings settings = appSettingsDAO.find();
            Map<Integer, MemberSubscription> latestByMember = subscriptionDAO.findLatestForBranch(scopedBranchId);
            request.setAttribute("members", members);
            request.setAttribute("latestByMember", latestByMember);
            request.setAttribute("reminderDays", settings.getReminderDaysBeforeExpiry());
            request.setAttribute("currentBranchId", scopedBranchId);
            request.setAttribute("currentBranchName", branchName(allBranches, scopedBranchId));
            request.getRequestDispatcher("/Forms/Transaction/members.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** A MANAGER/STAFF may only ever touch their own branch's members; an ADMIN may touch any branch's. */
    static boolean canAccess(User user, int branchId) {
        return user.isAdmin() || (user.getBranchId() != null && user.getBranchId() == branchId);
    }

    private String branchName(List<GymBranch> allBranches, int branchId) {
        for (GymBranch b : allBranches) {
            if (b.getBranchId() == branchId) {
                return b.getName();
            }
        }
        return "";
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessMembers()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "add": {
                    if (!user.canCreate()) {
                        Permissions.deny(request, response, "/members");
                        return;
                    }
                    Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
                    if (scopedBranchId == null) {
                        Permissions.deny(request, response, "/members");
                        return;
                    }
                    Member m = readForm(request);
                    m.setBranchId(scopedBranchId);
                    int newId = memberDAO.insert(m);
                    AuditLog.record(request, "CREATE", "MEMBER", newId, "Added member: " + m.getFullName());
                    break;
                }
                case "update": {
                    if (!user.canEdit()) {
                        Permissions.deny(request, response, "/members");
                        return;
                    }
                    int memberId = Integer.parseInt(request.getParameter("memberId"));
                    Member existing = memberDAO.findById(memberId);
                    if (existing == null || !canAccess(user, existing.getBranchId())) {
                        Permissions.deny(request, response, "/members");
                        return;
                    }
                    Member m = readForm(request);
                    m.setBranchId(existing.getBranchId()); // a member's branch never changes via edit
                    m.setMemberId(memberId);
                    memberDAO.update(m);
                    AuditLog.record(request, "UPDATE", "MEMBER", m.getMemberId(), "Updated member: " + m.getFullName());
                    break;
                }
                case "delete": {
                    if (!user.canDelete()) {
                        Permissions.deny(request, response, "/members");
                        return;
                    }
                    int memberId = Integer.parseInt(request.getParameter("memberId"));
                    Member existing = memberDAO.findById(memberId);
                    if (existing == null || !canAccess(user, existing.getBranchId())) {
                        Permissions.deny(request, response, "/members");
                        return;
                    }
                    // payments.subscription_id FK's member_subscriptions, so purge payments first.
                    paymentDAO.deleteByMember(memberId);
                    subscriptionDAO.deleteByMember(memberId);
                    memberDAO.delete(existing.getBranchId(), memberId);
                    AuditLog.record(request, "DELETE", "MEMBER", memberId, "Deleted member: " + existing.getFullName());
                    break;
                }
                default:
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        response.sendRedirect(request.getContextPath() + "/members");
    }

    private Member readForm(HttpServletRequest request) {
        Member m = new Member();
        m.setFullName(request.getParameter("fullName"));
        m.setPhone(request.getParameter("phone"));
        m.setEmail(request.getParameter("email"));
        m.setGender(request.getParameter("gender"));
        String dob = request.getParameter("dob");
        m.setDob(dob == null || dob.trim().isEmpty() ? null : LocalDate.parse(dob));
        m.setAddress(request.getParameter("address"));
        String joinDate = request.getParameter("joinDate");
        m.setJoinDate(joinDate == null || joinDate.trim().isEmpty() ? LocalDate.now() : LocalDate.parse(joinDate));
        String status = request.getParameter("status");
        m.setStatus(Member.INACTIVE.equals(status) ? Member.INACTIVE : Member.ACTIVE);
        return m;
    }
}
