<%@ page import="java.util.List, model.User, service.ReportService" %>
<% String pageTitle = "Reports"; %>
<%@ include file="../../header.jsp" %>

<%
    User loggedInUser = (User) session.getAttribute("user");
    if (Boolean.TRUE.equals(request.getAttribute("noBranch"))) {
%>
<div class="row">
<div class="col-12">
    <div class="alert alert-warning">
        No branches yet.
        <% if (loggedInUser.isAdmin()) { %>
            <a href="<%= request.getContextPath() %>/branches">Add one first</a> to see reports.
        <% } else { %>
            Ask your Admin to add a branch first.
        <% } %>
    </div>
</div>
</div>
<% } else {
        String type = (String) request.getAttribute("type");
        String start = (String) request.getAttribute("start");
        String end = (String) request.getAttribute("end");
        boolean reportIsAdmin = Boolean.TRUE.equals(request.getAttribute("isAdmin"));
%>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <ul class="nav nav-tabs">
            <li class="nav-item"><a class="nav-link <%= "due".equals(type) ? "active" : "" %>" href="?type=due">Membership Due/Expiring</a></li>
            <li class="nav-item"><a class="nav-link <%= "revenue".equals(type) ? "active" : "" %>" href="?type=revenue&start=<%= start %>&end=<%= end %>">Revenue / Collection</a></li>
            <% if (reportIsAdmin) { %>
            <li class="nav-item"><a class="nav-link <%= "branchSummary".equals(type) ? "active" : "" %>" href="?type=branchSummary">Branch-wise Active Members</a></li>
            <% } %>
        </ul>
        <a class="btn btn-outline-secondary btn-sm" href="<%= request.getContextPath() %>/reports?export=csv&type=<%= type %>&start=<%= start %>&end=<%= end %>">
            <i class="ti ti-download me-1"></i> Export CSV
        </a>
    </div>
</div>
</div>

<% if ("revenue".equals(type)) { %>
<div class="row">
<div class="col-12">
    <form method="get" class="d-flex align-items-center gap-2 mb-3 flex-wrap">
        <input type="hidden" name="type" value="revenue">
        <label class="form-label mb-0">From</label>
        <input type="date" class="form-control" style="max-width:170px;" name="start" value="<%= start %>">
        <label class="form-label mb-0">To</label>
        <input type="date" class="form-control" style="max-width:170px;" name="end" value="<%= end %>">
        <button type="submit" class="btn btn-outline-primary btn-sm">Go</button>
    </form>
</div>
</div>
<% } %>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <% if ("due".equals(type)) {
            List<ReportService.DueRow> rows = (List<ReportService.DueRow>) request.getAttribute("dueRows");
        %>
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light"><tr><th>Branch</th><th>Member</th><th>Phone</th><th>Status</th><th>Days</th><th>Current Plan</th><th>Last Payment</th></tr></thead>
            <tbody>
            <% if (rows.isEmpty()) { %>
                <tr><td colspan="7" class="text-secondary">No members are due or expiring right now.</td></tr>
            <% } for (ReportService.DueRow r : rows) { %>
                <tr class="align-middle">
                    <td><%= r.branchName %></td>
                    <td><%= r.member.getFullName() %></td>
                    <td><%= r.member.getPhone() == null ? "-" : r.member.getPhone() %></td>
                    <td><span class="badge bg-<%= r.status.getBadgeColor() %>-subtle text-<%= r.status.getBadgeColor() %>"><%= r.status.getLabel() %></span></td>
                    <td><%= r.status.getDaysRemaining() %></td>
                    <td><%= r.status.getCurrentSubscription() == null ? "-" : r.status.getCurrentSubscription().getPlanName() %></td>
                    <td><%= r.lastPaymentDate == null ? "-" : r.lastPaymentDate %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
        <% } else if ("revenue".equals(type)) {
            List<ReportService.RevenueRow> rows = (List<ReportService.RevenueRow>) request.getAttribute("revenueRows");
            java.math.BigDecimal total = (java.math.BigDecimal) request.getAttribute("revenueTotal");
        %>
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light"><tr><th>Date</th><th>Amount Collected</th></tr></thead>
            <tbody>
            <% for (ReportService.RevenueRow r : rows) { %>
                <tr class="align-middle<%= r.amount.signum() == 0 ? " text-secondary" : "" %>">
                    <td><%= r.date %></td>
                    <td class="<%= r.amount.signum() > 0 ? "text-success" : "" %>">&#8377; <%= r.amount %></td>
                </tr>
            <% } %>
            </tbody>
            <tfoot>
                <tr class="fw-bold"><td>Total</td><td>&#8377; <%= total %></td></tr>
            </tfoot>
        </table>
        <% } else if ("branchSummary".equals(type)) {
            List<ReportService.BranchSummaryRow> rows = (List<ReportService.BranchSummaryRow>) request.getAttribute("branchRows");
        %>
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light"><tr><th>Branch</th><th>Active</th><th>Expiring Soon</th><th>Expired</th><th>No Plan</th></tr></thead>
            <tbody>
            <% for (ReportService.BranchSummaryRow r : rows) { %>
                <tr class="align-middle">
                    <td><%= r.branchName %></td>
                    <td class="text-success"><%= r.active %></td>
                    <td class="text-warning"><%= r.expiringSoon %></td>
                    <td class="text-danger"><%= r.expired %></td>
                    <td class="text-secondary"><%= r.noPlan %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
        <% } %>
    </div>
</div>
</div>
<% } %>

<%@ include file="../../footer.jsp" %>
