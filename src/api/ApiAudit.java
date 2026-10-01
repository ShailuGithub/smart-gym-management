package api;

import dao.AuditLogDAO;
import model.User;

/**
 * Thin convenience wrapper so api.* servlets can log an action in one line, the API-side
 * equivalent of servlet.AuditLog (which is package-private to `servlet` and keys off the web
 * app's HttpSession, so it can't be reused here - the authenticated user comes from ApiSupport
 * instead of a session attribute).
 */
final class ApiAudit {

    private static final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private ApiAudit() {
    }

    static void record(User user, Integer branchId, String action, String entityType, Integer entityId, String description) {
        auditLogDAO.log(branchId, user.getUserId(), user.getUsername(), action, entityType, entityId, description);
    }
}
