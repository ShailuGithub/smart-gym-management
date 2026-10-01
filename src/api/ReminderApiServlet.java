package api;

import dao.GymBranchDAO;
import dao.MemberDAO;
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

/**
 * /api/reminders/due  (GET)  - branch-scoped members who are EXPIRING_SOON or EXPIRED.
 * /api/reminders/send (POST) - send a single reminder to one member.
 * Both delegate to service.ReminderService, the same shared logic servlet.ReminderServlet uses.
 */
@WebServlet({"/api/reminders/due", "/api/reminders/send"})
public class ReminderApiServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();
    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();

    private static final class SendInput {
        int memberId;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessReminders()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to Reminders.");
            return;
        }
        try {
            Integer branchId = ApiSupport.resolveConcreteBranchId(user, gymBranchDAO, request.getParameter("branchId"));
            if (branchId == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Specify a branchId query parameter (see GET /api/branches).");
                return;
            }
            JsonUtil.writeJson(response, ReminderService.findDueMembers(branchId));
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
        if (!user.canAccessReminders()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to Reminders.");
            return;
        }
        if (!"/api/reminders/send".equals(request.getServletPath())) {
            JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not found.");
            return;
        }
        SendInput input;
        try {
            input = JsonUtil.readJson(request, SendInput.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        if (input == null || input.memberId <= 0) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "memberId is required.");
            return;
        }
        try {
            Member member = memberDAO.findById(input.memberId);
            if (member == null || !ApiSupport.canAccessBranch(user, member.getBranchId())) {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Member not found.");
                return;
            }
            Reminder r = ReminderService.sendReminderTo(member);
            ApiAudit.record(user, member.getBranchId(), "CREATE", "REMINDER", r.getReminderId(),
                    "Sent " + r.getType().toLowerCase() + " reminder to " + member.getFullName() + " (" + r.getStatus() + ")");
            JsonUtil.writeJson(response, r);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
