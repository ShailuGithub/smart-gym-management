<%@ page import="java.util.List, model.GymBranch" %>
<% String pageTitle = "Branches"; %>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">Manage your gym branches</p>
        <a href="<%= request.getContextPath() %>/branches?view=form" class="btn btn-primary">
            <i class="ti ti-plus me-1"></i> Add Branch
        </a>
    </div>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light">
                <tr><th>Name</th><th>City</th><th>State</th><th>Phone</th><th>Email</th><th>Status</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                List<GymBranch> branches = (List<GymBranch>) request.getAttribute("branches");
                if (branches.isEmpty()) {
            %>
                <tr><td colspan="7" class="text-secondary">No branches yet - add your first one above.</td></tr>
            <%
                }
                for (GymBranch b : branches) {
            %>
                <tr class="align-middle">
                    <td><%= b.getName() %></td>
                    <td><%= b.getCity() == null ? "" : b.getCity() %></td>
                    <td><%= b.getState() == null ? "" : b.getState() %></td>
                    <td><%= b.getPhone() == null ? "" : b.getPhone() %></td>
                    <td><%= b.getEmail() == null ? "" : b.getEmail() %></td>
                    <td><span class="badge bg-<%= b.isActive() ? "success" : "secondary" %>-subtle text-<%= b.isActive() ? "success" : "secondary" %>"><%= b.getStatus() %></span></td>
                    <td>
                        <a href="<%= request.getContextPath() %>/branches?view=form&branchId=<%= b.getBranchId() %>"><i class="ti ti-edit"></i></a>
                        <form method="post" action="<%= request.getContextPath() %>/branches" class="d-inline"
                              onsubmit="return confirm('Delete this branch? This is only possible if it has no members or other records.');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="branchId" value="<%= b.getBranchId() %>">
                            <button type="submit" class="btn btn-link p-0 link-danger ms-2"><i class="ti ti-trash"></i></button>
                        </form>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
