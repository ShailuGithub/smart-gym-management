package servlet;

import csv.CsvUtil;
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
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reports screen: Membership Due/Expiring, Revenue/Collection, Branch-wise Active Members.
 * Each report respects the current branch scope (see BranchContext) except the branch-wise
 * summary, which always shows one row per branch (that's the point of it). Plain tables +
 * CSV export, no charting library - same approach as the reference project's ReportServlet.
 * Row-building/aggregation logic lives in service.ReportService, shared with api.ReportApiServlet.
 */
@WebServlet("/reports")
public class ReportServlet extends HttpServlet {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessReports()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            List<GymBranch> allBranches = gymBranchDAO.findAll();
            if (allBranches.isEmpty()) {
                request.setAttribute("noBranch", true);
                request.getRequestDispatcher("/Forms/Reports/reports.jsp").forward(request, response);
                return;
            }

            Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
            List<GymBranch> scope = ReportService.resolveScope(allBranches, scopedBranchId);

            String type = request.getParameter("type");
            type = type == null || type.trim().isEmpty() ? "due" : type;

            LocalDate start = parseOrDefault(request.getParameter("start"), LocalDate.now().withDayOfMonth(1));
            LocalDate end = parseOrDefault(request.getParameter("end"), LocalDate.now());

            if ("csv".equals(request.getParameter("export"))) {
                exportCsv(response, type, start, end, scope);
                return;
            }

            request.setAttribute("type", type);
            request.setAttribute("start", start.format(ISO));
            request.setAttribute("end", end.format(ISO));
            request.setAttribute("isAdmin", user.isAdmin());

            switch (type) {
                case "revenue":
                    request.setAttribute("revenueRows", ReportService.buildRevenueRows(scope, start, end));
                    request.setAttribute("revenueTotal", ReportService.sumRevenue(scope, start, end));
                    break;
                case "branchSummary":
                    // Always every branch regardless of the header switcher - a MANAGER only ever
                    // has one branch anyway, so this naturally degrades to a single row for them.
                    request.setAttribute("branchRows", ReportService.buildBranchSummaryRows(allBranches));
                    break;
                case "due":
                default:
                    request.setAttribute("dueRows", ReportService.buildDueRows(scope));
                    break;
            }
            request.getRequestDispatcher("/Forms/Reports/reports.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
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

    private void exportCsv(HttpServletResponse response, String type, LocalDate start, LocalDate end,
                            List<GymBranch> scope) throws IOException, SQLException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + type + "-report.csv");
        PrintWriter out = response.getWriter();
        switch (type) {
            case "revenue": {
                out.write(CsvUtil.writeRow("Date", "Amount Collected"));
                for (ReportService.RevenueRow r : ReportService.buildRevenueRows(scope, start, end)) {
                    out.write(CsvUtil.writeRow(r.date.toString(), r.amount.toString()));
                }
                out.write(CsvUtil.writeRow("Total", ReportService.sumRevenue(scope, start, end).toString()));
                break;
            }
            case "branchSummary": {
                out.write(CsvUtil.writeRow("Branch", "Active", "Expiring Soon", "Expired", "No Plan"));
                for (ReportService.BranchSummaryRow r : ReportService.buildBranchSummaryRows()) {
                    out.write(CsvUtil.writeRow(r.branchName, String.valueOf(r.active),
                            String.valueOf(r.expiringSoon), String.valueOf(r.expired), String.valueOf(r.noPlan)));
                }
                break;
            }
            case "due":
            default: {
                out.write(CsvUtil.writeRow("Branch", "Member", "Phone", "Status", "Days Remaining", "Current Plan", "Last Payment"));
                for (ReportService.DueRow r : ReportService.buildDueRows(scope)) {
                    out.write(CsvUtil.writeRow(r.branchName, r.member.getFullName(),
                            r.member.getPhone() == null ? "" : r.member.getPhone(), r.status.getLabel(),
                            String.valueOf(r.status.getDaysRemaining()),
                            r.status.getCurrentSubscription() == null ? "" : r.status.getCurrentSubscription().getPlanName(),
                            r.lastPaymentDate == null ? "" : r.lastPaymentDate.toString()));
                }
                break;
            }
        }
        out.flush();
    }
}
