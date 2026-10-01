<%@ page import="java.util.List, model.GymBranch" %>
<%
    String pageTitle = "Add User";
    List<GymBranch> branches = (List<GymBranch>) request.getAttribute("branches");
%>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">New user account</p>
        <a href="<%= request.getContextPath() %>/users" class="btn btn-outline-secondary">Back to List</a>
    </div>
</div>
</div>

<% if (request.getAttribute("error") != null) { %>
<div class="row"><div class="col-12"><div class="alert alert-danger py-2"><%= request.getAttribute("error") %></div></div></div>
<% } %>

<div class="row">
<div class="col-12">
    <div class="card">
        <div class="card-body p-4">
            <form method="post" action="<%= request.getContextPath() %>/users" id="userForm">
                <input type="hidden" name="action" value="create">
                <div class="row">
                    <div class="col-md-3 mb-3">
                        <label class="form-label">Full Name</label>
                        <input type="text" class="form-control" name="fullName" placeholder="Enter Name" required>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label class="form-label">Username</label>
                        <input type="text" class="form-control" name="username" placeholder="Enter Username" required>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control" name="email" placeholder="Enter Email" required>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label class="form-label">Password</label>
                        <input type="password" class="form-control" name="password" required minlength="6">
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-3 mb-3">
                        <label class="form-label">Role</label>
                        <select class="form-select" name="role" id="roleSelect" onchange="onRoleChange()">
                            <option value="MANAGER" selected>Manager</option>
                            <option value="STAFF">Staff</option>
                            <option value="ADMIN">Owner/Admin</option>
                        </select>
                    </div>
                    <div class="col-md-3 mb-3" id="managerBranchField">
                        <label class="form-label">Branch</label>
                        <select class="form-select" name="branchId">
                            <% for (GymBranch b : branches) { %>
                            <option value="<%= b.getBranchId() %>"><%= b.getName() %></option>
                            <% } %>
                        </select>
                    </div>
                    <div class="col-md-6 mb-3" id="managerFields">
                        <label class="form-label">Permissions</label>
                        <div class="d-flex gap-3">
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="canCreate" id="permCreate" checked>
                                <label class="form-check-label" for="permCreate">Can Create</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="canEdit" id="permEdit" checked>
                                <label class="form-check-label" for="permEdit">Can Edit</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="canDelete" id="permDelete">
                                <label class="form-check-label" for="permDelete">Can Delete</label>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary">Add User</button>
                    <a href="<%= request.getContextPath() %>/users" class="btn btn-secondary">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</div>
</div>

<script>
function onRoleChange() {
    var isBranchScoped = document.getElementById('roleSelect').value !== 'ADMIN';
    document.getElementById('managerFields').style.display = isBranchScoped ? '' : 'none';
    document.getElementById('managerBranchField').style.display = isBranchScoped ? '' : 'none';
}
</script>

<%@ include file="../../footer.jsp" %>
