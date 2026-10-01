package api;

import dao.GymBranchDAO;
import model.GymBranch;
import model.User;
import service.ReportService;

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

/**
 * Read-only report endpoints, reusing the exact same row-building/aggregation logic as the web
 * Reports screen (see service.ReportService, shared with servlet.ReportServlet) so the two never
 * drift apart. No CSV export here - that's a web-only convenience for now.
 *
 * GET /api/reports/due            -> members currently EXPIRING_SOON/EXPIRED, branch-scoped
 * GET /api/reports/revenue        -> daily collection totals for a date range (?start=&end=, default = this month), branch-scoped
 * GET /api/reports/branch-summary -> per-branch Active/Expiring/Expired/No-Plan counts, admin-only (every branch, like the web version)
 */
@WebServlet("/api/reports/*")
public class ReportApiServlet extends HttpServlet {

    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();

    private static final class RevenueResponse {
        final List<ReportService.RevenueRow> rows;
        final BigDecimal total;

        RevenueResponse(List<ReportService.RevenueRow> rows, BigDecimal total) {
            this.rows = rows;
            this.total = total;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessReports()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to Reports.");
            return;
        }
        String pathInfo = request.getPathInfo(); // e.g. "/due", "/revenue", "/branch-summary"
        String reportType = pathInfo == null ? "" : pathInfo.replace("/", "");
        try {
            switch (reportType) {
                case "due": {
                    List<GymBranch> scope = scopedBranches(user, request.getParameter("branchId"));
                    JsonUtil.writeJson(response, ReportService.buildDueRows(scope));
                    return;
                }
                case "revenue": {
                    List<GymBranch> scope = scopedBranches(user, request.getParameter("branchId"));
                    LocalDate start = parseOrDefault(request.getParameter("start"), LocalDate.now().withDayOfMonth(1));
                    LocalDate end = parseOrDefault(request.getParameter("end"), LocalDate.now());
                    List<ReportService.RevenueRow> rows = ReportService.buildRevenueRows(scope, start, end);
                    BigDecimal total = ReportService.sumRevenue(scope, start, end);
                    JsonUtil.writeJson(response, new RevenueResponse(rows, total));
                    return;
                }
                case "branch-summary": {
                    if (!user.isAdmin()) {
                        JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "Branch-wise summary is Admin-only.");
                        return;
                    }
                    JsonUtil.writeJson(response, ReportService.buildBranchSummaryRows());
                    return;
                }
                default:
                    JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Unknown report - use /api/reports/due, /revenue, or /branch-summary.");
            }
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    /** Branch(es) this user is allowed to see for a scoped report - same rule as ApiSupport.resolveBranchScope. */
    private List<GymBranch> scopedBranches(User user, String branchIdParam) throws SQLException {
        List<GymBranch> allBranches = gymBranchDAO.findAll();
        if (!user.isAdmin()) {
            return ReportService.resolveScope(allBranches, user.getBranchId());
        }
        if (branchIdParam != null && !branchIdParam.trim().isEmpty()) {
            try {
                return ReportService.resolveScope(allBranches, Integer.parseInt(branchIdParam.trim()));
            } catch (NumberFormatException ignored) {
                // fall through to "all branches"
            }
        }
        return allBranches;
    }

    private LocalDate parseOrDefault(String param, LocalDate fallback) {
        if (param == null || param.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LocalDate.parse(param.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
