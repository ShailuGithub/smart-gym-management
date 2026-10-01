<%@ page import="java.util.List, model.User, model.GymBranch" %>
<% String pageTitle = "Manage Users"; %>
<%@ include file="../../header.jsp" %>

<%
    List<GymBranch> branches = (List<GymBranch>) request.getAttribute("branches");
%>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">Owner/Admin, Manager &amp; Staff accounts</p>
        <a href="<%= request.getContextPath() %>/users?view=form" class="btn btn-primary">
            <i class="ti ti-plus me-1"></i> Add User
        </a>
    </div>
</div>
</div>

<%
    String errParam = request.getParameter("error");
    if ("selfDelete".equals(errParam)) { %>
<div class="row"><div class="col-12"><div class="alert alert-danger py-2">You can't delete your own account.</div></div></div>
<% } else if ("lastAdmin".equals(errParam)) { %>
<div class="row"><div class="col-12"><div class="alert alert-danger py-2">You can't delete the last remaining Admin account.</div></div></div>
<% } %>

<div class="row">
<div class="col-12">
    <p class="text-secondary small">
        An Owner/Admin always has full access to every branch. Every other role (Manager, Staff)
        is assigned to one branch and is limited to whichever of Create / Edit / Delete you grant below -
        leave all three unchecked for a view-only account.
    </p>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <table class="table mb-0 text-nowrap table-hover align-middle">
            <thead class="table-light border-light">
                <tr><th>Username</th><th>Full Name</th><th>Email</th><th>Role</th><th>Branch</th>
                    <th class="text-center">Create</th><th class="text-center">Edit</th><th class="text-center">Delete</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                List<User> users = (List<User>) request.getAttribute("users");
                for (User u : users) {
                    boolean isBranchScoped = !u.isAdmin();
                    String branchName = "-";
                    if (u.getBranchId() != null) {
                        for (GymBranch b : branches) {
                            if (b.getBranchId() == u.getBranchId()) { branchName = b.getName(); break; }
                        }
                    }
            %>
                <tr class="align-middle">
                    <td><%= u.getUsername() %></td>
                    <td><%= u.getFullName() == null ? "" : u.getFullName() %></td>
                    <td><%= u.getEmail() == null ? "" : u.getEmail() %></td>
                    <td><span class="badge bg-<%= u.isAdmin() ? "primary" : "secondary" %>-subtle text-<%= u.isAdmin() ? "primary" : "secondary" %>"><%= u.getRoleLabel() %></span></td>
                    <td><%= isBranchScoped ? branchName : "All Branches" %></td>
                <% if (isBranchScoped) { %>
                    <form method="post" action="<%= request.getContextPath() %>/users" id="permForm<%= u.getUserId() %>">
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="userId" value="<%= u.getUserId() %>">
                        <input type="hidden" name="fullName" value="<%= u.getFullName() == null ? "" : u.getFullName() %>">
                        <input type="hidden" name="email" value="<%= u.getEmail() == null ? "" : u.getEmail() %>">
                        <input type="hidden" name="branchId" value="<%= u.getBranchId() %>">
                    </form>
                    <td class="text-center">
                        <input type="checkbox" form="permForm<%= u.getUserId() %>" name="canCreate" <%= u.isPermCreate() ? "checked" : "" %>>
                    </td>
                    <td class="text-center">
                        <input type="checkbox" form="permForm<%= u.getUserId() %>" name="canEdit" <%= u.isPermEdit() ? "checked" : "" %>>
                    </td>
                    <td class="text-center">
                        <input type="checkbox" form="permForm<%= u.getUserId() %>" name="canDelete" <%= u.isPermDelete() ? "checked" : "" %>>
                    </td>
                    <td class="text-nowrap">
                        <button type="submit" form="permForm<%= u.getUserId() %>" class="btn btn-sm btn-outline-primary">
                            <i class="ti ti-check"></i> Save
                        </button>
                        <form method="post" action="<%= request.getContextPath() %>/users" class="d-inline"
                              onsubmit="return confirm('Remove this user?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="userId" value="<%= u.getUserId() %>">
                            <button type="submit" class="btn btn-sm btn-outline-danger"><i class="ti ti-trash"></i></button>
                        </form>
                    </td>
                <% } else { %>
                    <td class="text-center"><i class="ti ti-check text-success"></i></td>
                    <td class="text-center"><i class="ti ti-check text-success"></i></td>
                    <td class="text-center"><i class="ti ti-check text-success"></i></td>
                    <td class="text-nowrap">
                        <form method="post" action="<%= request.getContextPath() %>/users" class="d-inline"
                              onsubmit="return confirm('Remove this admin account?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="userId" value="<%= u.getUserId() %>">
                            <button type="submit" class="btn btn-sm btn-outline-danger"><i class="ti ti-trash"></i></button>
                        </form>
                    </td>
                <% } %>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
