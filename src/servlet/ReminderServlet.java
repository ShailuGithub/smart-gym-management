package servlet;

import dao.AppSettingsDAO;
import dao.GymBranchDAO;
import dao.MemberDAO;
import model.GymBranch;
import model.Member;
import model.Reminder;
import model.User;
import service.ReminderService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Lists members who are EXPIRING_SOON or EXPIRED for the branch currently in scope (see
 * BranchContext) and lets staff send a reminder - one at a time, or "Send to All". The due
 * computation and actual send/log logic live in service.ReminderService (shared with
 * api.ReminderApiServlet) - this servlet is just the web request/response/permission/audit-log
 * wiring around it.
 */
@WebServlet("/reminders")
public class ReminderServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();
    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessReminders()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            List<GymBranch> allBranches = gymBranchDAO.findAll();
            if (allBranches.isEmpty()) {
                request.setAttribute("noBranch", true);
                request.getRequestDispatcher("/Forms/Transaction/reminders.jsp").forward(request, response);
                return;
            }
            Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
            if (scopedBranchId == null) {
                request.setAttribute("selectBranchFirst", true);
                request.setAttribute("allBranches", allBranches);
                request.getRequestDispatcher("/Forms/Transaction/reminders.jsp").forward(request, response);
                return;
            }

            request.setAttribute("dueMembers", ReminderService.findDueMembers(scopedBranchId));
            request.setAttribute("currentBranchId", scopedBranchId);
            request.setAttribute("currentBranchName", branchName(allBranches, scopedBranchId));
            request.setAttribute("smsEnabled", appSettingsDAO.find().isSmsEnabled());
            request.getRequestDispatcher("/Forms/Transaction/reminders.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessReminders()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        String action = request.getParameter("action");
        try {
            Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
            if (scopedBranchId == null) {
                Permissions.deny(request, response, "/reminders");
                return;
            }
            switch (action == null ? "" : action) {
                case "send": {
                    int memberId = Integer.parseInt(request.getParameter("memberId"));
                    Member member = memberDAO.findById(memberId);
                    if (member == null || !MemberServlet.canAccess(user, member.getBranchId())) {
                        Permissions.deny(request, response, "/reminders");
                        return;
                    }
                    // A single explicit "Send" always goes through, even if this member was
                    // already reminded today - that's a deliberate staff action, unlike "Send to All".
                    logReminder(request, ReminderService.sendReminderTo(member), member);
                    break;
                }
                case "sendAll": {
                    // Recomputed server-side, never trusted from the client - "send to all" always
                    // means "everyone currently due for this branch", not whatever ids happened to
                    // be on the page. Members already reminded today are skipped to avoid spamming
                    // on repeated clicks.
                    for (ReminderService.DueMember dm : ReminderService.findDueMembers(scopedBranchId)) {
                        if (!ReminderService.remindedToday(dm.member.getMemberId())) {
                            logReminder(request, ReminderService.sendReminderTo(dm.member), dm.member);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        response.sendRedirect(request.getContextPath() + "/reminders");
    }

    private void logReminder(HttpServletRequest request, Reminder r, Member member) {
        AuditLog.record(request, member.getBranchId(), "CREATE", "REMINDER", r.getReminderId(),
                "Sent " + r.getType().toLowerCase() + " reminder to " + member.getFullName() + " (" + r.getStatus() + ")");
    }

    private String branchName(List<GymBranch> allBranches, int branchId) {
        for (GymBranch b : allBranches) {
            if (b.getBranchId() == branchId) {
                return b.getName();
            }
        }
        return "";
    }
}
