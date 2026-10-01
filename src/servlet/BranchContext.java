package servlet;

import model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Resolves which gym branch is "in scope" for the current request.
 * Every non-ADMIN role (Manager, Staff) is always locked to the one branch their account
 * belongs to.
 * An ADMIN (Owner) can switch between branches (via the sidebar branch-switcher, which links
 * with ?branchId=) or view "All Branches" (aggregate) - the chosen branch is remembered in the
 * session so it persists across page navigation without needing ?branchId= on every link.
 *
 * Public (unlike the other same-package helpers here) because plain JSPs - which several
 * modules use directly instead of going through a servlet (dashboard.jsp, reports.jsp) - need
 * to call this too, and a JSP-compiled class can only see public members of another package.
 */
public final class BranchContext {

    private static final String SESSION_KEY = "selectedBranchId";

    private BranchContext() {
    }

    /** The branch currently in scope, or null if an ADMIN has chosen "All Branches" (or hasn't picked one yet). */
    public static Integer currentBranchIdOrNull(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        // Every non-ADMIN role (Manager, Staff) is tied to exactly one branch - only an ADMIN
        // can switch branches or view "All Branches".
        if (!user.isAdmin()) {
            return user.getBranchId();
        }
        HttpSession session = request.getSession();
        String param = request.getParameter("branchId");
        if (param != null) {
            if (param.trim().isEmpty() || "all".equalsIgnoreCase(param)) {
                session.removeAttribute(SESSION_KEY);
                return null;
            }
            try {
                int branchId = Integer.parseInt(param.trim());
                session.setAttribute(SESSION_KEY, branchId);
                return branchId;
            } catch (NumberFormatException e) {
                // fall through to whatever's already in session
            }
        }
        return (Integer) session.getAttribute(SESSION_KEY);
    }

    /**
     * Same as currentBranchIdOrNull(), but for screens that require exactly one concrete branch
     * (no aggregate "All Branches" view) - falls back to the given default (e.g. the first
     * branch in the caller's list) when nothing is selected yet.
     */
    public static int currentBranchIdOrDefault(HttpServletRequest request, int defaultBranchId) {
        Integer branchId = currentBranchIdOrNull(request);
        return branchId != null ? branchId : defaultBranchId;
    }
}
