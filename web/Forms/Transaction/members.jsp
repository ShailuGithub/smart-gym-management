<%@ page import="java.util.List, java.util.Map, model.Member, model.MemberSubscription, model.MembershipStatus, model.User, model.GymBranch" %>
<% String pageTitle = "Members"; %>
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
            <a href="<%= request.getContextPath() %>/branches">Add one first</a> before adding members.
        <% } else { %>
            Ask your Admin to add a branch before adding members.
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
        <i class="ti ti-building me-1"></i> Pick a branch from the switcher above (top right) to view or add its members -
        with "All Branches" selected there's no single branch to add a new member into.
        <div class="d-flex flex-wrap gap-2 mt-2">
            <% for (GymBranch b : allBranches) { %>
                <a class="btn btn-sm btn-outline-primary" href="?branchId=<%= b.getBranchId() %>"><%= b.getName() %></a>
            <% } %>
        </div>
    </div>
</div>
</div>
<% } else {
        List<Member> members = (List<Member>) request.getAttribute("members");
        Map<Integer, MemberSubscription> latestByMember = (Map<Integer, MemberSubscription>) request.getAttribute("latestByMember");
        Integer reminderDays = (Integer) request.getAttribute("reminderDays");
        String currentBranchName = (String) request.getAttribute("currentBranchName");
%>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">
            Manage members in
            <span class="badge bg-primary-subtle text-primary"><i class="ti ti-building me-1"></i><%= currentBranchName %></span>
        </p>
        <% if (loggedInUser.canCreate()) { %>
        <a href="<%= request.getContextPath() %>/members?view=form" class="btn btn-primary">
            <i class="ti ti-plus me-1"></i> Add Member
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
                <tr><th>Name</th><th>Phone</th><th>Joined</th><th>Membership</th><th>Status</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                if (members.isEmpty()) {
            %>
                <tr><td colspan="6" class="text-secondary">No members yet - add your first one above.</td></tr>
            <%
                }
                for (Member m : members) {
                    MembershipStatus ms = MembershipStatus.of(m.getMemberId(), latestByMember.get(m.getMemberId()), reminderDays);
            %>
                <tr class="align-middle">
                    <td><a href="<%= request.getContextPath() %>/members?memberId=<%= m.getMemberId() %>"><%= m.getFullName() %></a></td>
                    <td><%= m.getPhone() == null ? "" : m.getPhone() %></td>
                    <td><%= m.getJoinDate() %></td>
                    <td>
                        <span class="badge bg-<%= ms.getBadgeColor() %>-subtle text-<%= ms.getBadgeColor() %>"><%= ms.getLabel() %></span>
                        <% if (ms.getCurrentSubscription() != null) { %>
                            <span class="text-secondary small"><%= ms.getCurrentSubscription().getPlanName() %> &middot; till <%= ms.getCurrentSubscription().getEndDate() %></span>
                        <% } %>
                    </td>
                    <td><span class="badge bg-<%= m.isActive() ? "success" : "secondary" %>-subtle text-<%= m.isActive() ? "success" : "secondary" %>"><%= m.getStatus() %></span></td>
                    <td>
                        <% if (loggedInUser.canEdit()) { %>
                        <a href="<%= request.getContextPath() %>/members?view=form&memberId=<%= m.getMemberId() %>"><i class="ti ti-edit"></i></a>
                        <% } %>
                        <% if (loggedInUser.canDelete()) { %>
                        <form method="post" action="<%= request.getContextPath() %>/members" class="d-inline"
                              onsubmit="return confirm('Delete this member and their membership history?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="memberId" value="<%= m.getMemberId() %>">
                            <button type="submit" class="btn btn-link p-0 link-danger ms-2"><i class="ti ti-trash"></i></button>
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
