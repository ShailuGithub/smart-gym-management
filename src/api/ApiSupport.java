package api;

import dao.GymBranchDAO;
import model.GymBranch;
import model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for the JSON API servlets - the stateless-request equivalents of
 * servlet.Permissions/servlet.BranchContext, which both key off the web app's HttpSession and so
 * can't be reused here. The authenticated user is read from the "apiUser" request attribute that
 * api.ApiAuthFilter sets after validating the bearer token.
 */
final class ApiSupport {

    private ApiSupport() {
    }

    /** The authenticated user for this request, or null if (unexpectedly) ApiAuthFilter didn't run. */
    static User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute("apiUser");
    }

    /** Writes a 401 - defense in depth for the case where currentUser() unexpectedly returns null. */
    static void requireUser(HttpServletResponse response) throws IOException {
        JsonUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated.");
    }

    /** Same rule as servlet.MemberServlet.canAccess(): an ADMIN may touch any branch's data, a MANAGER/STAFF only their own. */
    static boolean canAccessBranch(User user, int branchId) {
        return user.isAdmin() || (user.getBranchId() != null && user.getBranchId() == branchId);
    }

    /**
     * Resolves the set of branch ids visible to this user for an aggregate endpoint (e.g. the
     * dashboard): every branch for an ADMIN (optionally narrowed to one via ?branchId=), or just
     * their own single branch for a MANAGER/STAFF - a client-supplied branchId is never trusted to
     * widen a non-admin's scope.
     */
    static List<Integer> resolveBranchScope(User user, GymBranchDAO branchDAO, String branchIdParam) throws SQLException {
        List<Integer> scope = new ArrayList<>();
        if (!user.isAdmin()) {
            if (user.getBranchId() != null) {
                scope.add(user.getBranchId());
            }
            return scope;
        }
        if (branchIdParam != null && !branchIdParam.trim().isEmpty()) {
            try {
                int branchId = Integer.parseInt(branchIdParam.trim());
                GymBranch branch = branchDAO.findById(branchId);
                if (branch != null) {
                    scope.add(branchId);
                    return scope;
                }
            } catch (NumberFormatException ignored) {
                // fall through to "all branches"
            }
        }
        for (GymBranch b : branchDAO.findAll()) {
            scope.add(b.getBranchId());
        }
        return scope;
    }

    /**
     * Resolves exactly one concrete branch for an endpoint that (like the web app's branch-scoped
     * screens) can't operate on an aggregate "all branches" view - a MANAGER/STAFF's own branch, or
     * for an ADMIN, the branchId param if given. Returns null (write a 400 yourself) if an ADMIN
     * didn't specify one and more than one branch exists, or if the branchId param doesn't resolve
     * to a real branch.
     */
    static Integer resolveConcreteBranchId(User user, GymBranchDAO branchDAO, String branchIdParam) throws SQLException {
        if (!user.isAdmin()) {
            return user.getBranchId();
        }
        if (branchIdParam != null && !branchIdParam.trim().isEmpty()) {
            try {
                int branchId = Integer.parseInt(branchIdParam.trim());
                return branchDAO.findById(branchId) != null ? branchId : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        List<GymBranch> all = branchDAO.findAll();
        return all.size() == 1 ? all.get(0).getBranchId() : null;
    }
}
