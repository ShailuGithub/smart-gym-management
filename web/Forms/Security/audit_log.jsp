<%@ page import="java.util.List, model.AuditLogEntry" %>
<% String pageTitle = "Audit Log"; %>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-12">
    <p class="text-secondary small">Most recent 200 create/edit/delete actions, newest first. Use the branch switcher above to scope this to one branch, or view "All Branches".</p>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light">
                <tr><th>Date/Time</th><th>User</th><th>Action</th><th>Type</th><th>Details</th></tr>
            </thead>
            <tbody>
            <%
                List<AuditLogEntry> entries = (List<AuditLogEntry>) request.getAttribute("entries");
                if (entries.isEmpty()) {
            %>
                <tr><td colspan="5" class="text-secondary">No activity recorded yet.</td></tr>
            <%
                }
                for (AuditLogEntry e : entries) {
                    String badgeColor = "CREATE".equals(e.getAction()) ? "success"
                            : "DELETE".equals(e.getAction()) ? "danger" : "warning";
            %>
                <tr class="align-middle">
                    <td><%= e.getCreatedAt() %></td>
                    <td><%= e.getUsername() %></td>
                    <td><span class="badge bg-<%= badgeColor %>-subtle text-<%= badgeColor %>"><%= e.getAction() %></span></td>
                    <td><%= e.getEntityType() %></td>
                    <td class="text-wrap"><%= e.getDescription() == null ? "" : e.getDescription() %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
