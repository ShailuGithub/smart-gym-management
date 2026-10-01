<%@ page import="java.util.List, model.Payment, model.User, model.GymBranch" %>
<% String pageTitle = "Payments"; %>
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
            <a href="<%= request.getContextPath() %>/branches">Add one first</a> before recording payments.
        <% } else { %>
            Ask your Admin to add a branch before recording payments.
        <% } %>
    </div>
</div>
</div>
<% } else if (Boolean.TRUE.equals(request.getAttribute("selectBranchFirst"))) {
        List<GymBranch> allBranches = (List<GymBranch>) request.getAttribute("allBranches");
%>
<div class="row">
<div class="col-12">
    <div class="alert alert-info">
        <i class="ti ti-building me-1"></i> Pick a branch from the switcher above (top right) to view or record its payments -
        with "All Branches" selected there's no single branch to record a new payment against.
        <div class="d-flex flex-wrap gap-2 mt-2">
            <% for (GymBranch b : allBranches) { %>
                <a class="btn btn-sm btn-outline-primary" href="?branchId=<%= b.getBranchId() %>"><%= b.getName() %></a>
            <% } %>
        </div>
    </div>
</div>
</div>
<% } else {
        List<Payment> payments = (List<Payment>) request.getAttribute("payments");
        String currentBranchName = (String) request.getAttribute("currentBranchName");
%>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">
            Payments collected at
            <span class="badge bg-primary-subtle text-primary"><i class="ti ti-building me-1"></i><%= currentBranchName %></span>
        </p>
        <% if (loggedInUser.canCreate()) { %>
        <a href="<%= request.getContextPath() %>/payments?view=form" class="btn btn-primary">
            <i class="ti ti-plus me-1"></i> Record Payment
        </a>
        <% } %>
    </div>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <table class="table mb-0 text-nowrap table-hover align-middle">
            <thead class="table-light border-light">
                <tr><th>Date</th><th>Member</th><th>Amount</th><th>Mode</th><th>Reference</th><th>Remarks</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                if (payments.isEmpty()) {
            %>
                <tr><td colspan="7" class="text-secondary">No payments recorded yet.</td></tr>
            <%
                }
                for (Payment p : payments) {
            %>
                <tr class="align-middle">
                    <td><%= p.getPaymentDate() %></td>
                    <td><a href="<%= request.getContextPath() %>/members?memberId=<%= p.getMemberId() %>"><%= p.getMemberName() %></a></td>
                    <td>&#8377; <%= p.getAmount() %></td>
                    <td><%= p.getMode() == null ? "-" : p.getMode() %></td>
                    <td><%= p.getReferenceNo() == null ? "-" : p.getReferenceNo() %></td>
                    <td><%= p.getRemarks() == null ? "-" : p.getRemarks() %></td>
                    <td>
                        <% if (loggedInUser.canDelete()) { %>
                        <form method="post" action="<%= request.getContextPath() %>/payments" class="d-inline"
                              onsubmit="return confirm('Delete this payment record?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="paymentId" value="<%= p.getPaymentId() %>">
                            <button type="submit" class="btn btn-link p-0 link-danger"><i class="ti ti-trash"></i></button>
                        </form>
                        <% } %>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
</div>
<% } %>

<%@ include file="../../footer.jsp" %>
