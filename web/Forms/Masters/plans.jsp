<%@ page import="java.util.List, model.MembershipPlan, model.User" %>
<% String pageTitle = "Membership Plans"; %>
<%@ include file="../../header.jsp" %>

<%
    User loggedInUser = (User) session.getAttribute("user");
    List<MembershipPlan> plans = (List<MembershipPlan>) request.getAttribute("plans");
%>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">Plans are shared across every branch</p>
        <% if (loggedInUser.canCreate()) { %>
        <a href="<%= request.getContextPath() %>/plans?view=form" class="btn btn-primary">
            <i class="ti ti-plus me-1"></i> Add Plan
        </a>
        <% } %>
    </div>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light">
                <tr><th>Name</th><th>Duration</th><th>Price</th><th>Description</th><th>Status</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                if (plans.isEmpty()) {
            %>
                <tr><td colspan="6" class="text-secondary">No plans yet - add your first one above.</td></tr>
            <%
                }
                for (MembershipPlan p : plans) {
            %>
                <tr class="align-middle">
                    <td><%= p.getName() %></td>
                    <td><%= p.getDurationDays() %> days</td>
                    <td>&#8377; <%= p.getPrice() %></td>
                    <td><%= p.getDescription() == null ? "" : p.getDescription() %></td>
                    <td><span class="badge bg-<%= p.isActive() ? "success" : "secondary" %>-subtle text-<%= p.isActive() ? "success" : "secondary" %>"><%= p.getStatus() %></span></td>
                    <td>
                        <% if (loggedInUser.canEdit()) { %>
                        <a href="<%= request.getContextPath() %>/plans?view=form&planId=<%= p.getPlanId() %>"><i class="ti ti-edit"></i></a>
                        <% } %>
                        <% if (loggedInUser.isAdmin()) { %>
                        <form method="post" action="<%= request.getContextPath() %>/plans" class="d-inline"
                              onsubmit="return confirm('Delete this plan? This is only possible if no member has ever been assigned it.');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="planId" value="<%= p.getPlanId() %>">
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

<%@ include file="../../footer.jsp" %>
