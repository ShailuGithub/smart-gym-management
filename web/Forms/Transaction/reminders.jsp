<%@ page import="java.util.List, model.GymBranch, model.User, service.ReminderService.DueMember" %>
<% String pageTitle = "Reminders"; %>
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
            <a href="<%= request.getContextPath() %>/branches">Add one first</a>.
        <% } else { %>
            Ask your Admin to add a branch.
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
        <i class="ti ti-building me-1"></i> Pick a branch from the switcher above (top right) to view its due/expiring members -
        with "All Branches" selected there's no single branch to send reminders for.
        <div class="d-flex flex-wrap gap-2 mt-2">
            <% for (GymBranch b : allBranches) { %>
                <a class="btn btn-sm btn-outline-primary" href="?branchId=<%= b.getBranchId() %>"><%= b.getName() %></a>
            <% } %>
        </div>
    </div>
</div>
</div>
<% } else {
        List<DueMember> dueMembers = (List<DueMember>) request.getAttribute("dueMembers");
        String currentBranchName = (String) request.getAttribute("currentBranchName");
        Boolean smsEnabled = (Boolean) request.getAttribute("smsEnabled");
%>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-3 gap-3">
        <p class="mb-0 text-secondary">
            Members due or expiring soon in
            <span class="badge bg-primary-subtle text-primary"><i class="ti ti-building me-1"></i><%= currentBranchName %></span>
        </p>
        <% if (!dueMembers.isEmpty()) { %>
        <form method="post" action="<%= request.getContextPath() %>/reminders"
              onsubmit="return confirm('Send a reminder to all ' + <%= dueMembers.size() %> + ' member(s) below (skipping anyone already reminded today)?');">
            <input type="hidden" name="action" value="sendAll">
            <button type="submit" class="btn btn-primary"><i class="ti ti-send me-1"></i> Send to All</button>
        </form>
        <% } %>
    </div>
    <% if (Boolean.FALSE.equals(smsEnabled)) { %>
    <div class="alert alert-secondary py-2">
        <i class="ti ti-info-circle me-1"></i> SMS sending is disabled in
        <a href="<%= request.getContextPath() %>/settings">Settings</a> - reminders below will only be logged, not actually sent.
    </div>
    <% } %>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <table class="table mb-0 text-nowrap table-hover align-middle">
            <thead class="table-light border-light">
                <tr><th>Name</th><th>Phone</th><th>Status</th><th>Plan</th><th>Last Reminder</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                if (dueMembers.isEmpty()) {
            %>
                <tr><td colspan="6" class="text-secondary">No members are due or expiring soon right now.</td></tr>
            <%
                }
                for (DueMember dm : dueMembers) {
            %>
                <tr class="align-middle">
                    <td><a href="<%= request.getContextPath() %>/members?memberId=<%= dm.member.getMemberId() %>"><%= dm.member.getFullName() %></a></td>
                    <td><%= dm.member.getPhone() == null ? "" : dm.member.getPhone() %></td>
                    <td>
                        <span class="badge bg-<%= dm.status.getBadgeColor() %>-subtle text-<%= dm.status.getBadgeColor() %>"><%= dm.status.getLabel() %></span>
                        <span class="text-secondary small">
                            <%= dm.status.isExpired() ? "expired " + Math.abs(dm.status.getDaysRemaining()) + " day(s) ago"
                                                       : dm.status.getDaysRemaining() + " day(s) left" %>
                        </span>
                    </td>
                    <td><%= dm.status.getCurrentSubscription() == null ? "-" : dm.status.getCurrentSubscription().getPlanName() %></td>
                    <td>
                        <% if (dm.lastReminder == null) { %>
                            <span class="text-secondary small">Never</span>
                        <% } else { %>
                            <span class="badge bg-<%= dm.lastReminder.getBadgeColor() %>-subtle text-<%= dm.lastReminder.getBadgeColor() %>"><%= dm.lastReminder.getStatus() %></span>
                            <span class="text-secondary small"><%= dm.lastReminder.getSentAt() %></span>
                        <% } %>
                    </td>
                    <td>
                        <form method="post" action="<%= request.getContextPath() %>/reminders" class="d-inline">
                            <input type="hidden" name="action" value="send">
                            <input type="hidden" name="memberId" value="<%= dm.member.getMemberId() %>">
                            <button type="submit" class="btn btn-sm btn-outline-primary"><i class="ti ti-send me-1"></i> Send</button>
                        </form>
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
