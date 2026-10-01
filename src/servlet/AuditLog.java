package servlet;

import dao.AuditLogDAO;
import model.User;

import javax.servlet.http.HttpServletRequest;

/** Thin convenience wrapper so servlets can log an action in one line without wiring up a DAO each time. */
final class AuditLog {

    private static final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private AuditLog() {
    }

    /** Logs against the branch currently in scope for this request (see BranchContext) - null for branch-less actions (e.g. managing branches themselves, or users). */
    static void record(HttpServletRequest request, String action, String entityType, Integer entityId,
                        String description) {
        User user = Permissions.userOf(request);
        Integer branchId = BranchContext.currentBranchIdOrNull(request);
        auditLogDAO.log(branchId, user.getUserId(), user.getUsername(), action, entityType, entityId, description);
    }

    /** Logs against an explicit branch - used when the affected record's branch isn't the same as the request's current scope. */
    static void record(HttpServletRequest request, Integer branchId, String action, String entityType, Integer entityId,
                        String description) {
        User user = Permissions.userOf(request);
        auditLogDAO.log(branchId, user.getUserId(), user.getUsername(), action, entityType, entityId, description);
    }
}
