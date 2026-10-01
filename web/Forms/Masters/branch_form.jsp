<%@ page import="model.GymBranch" %>
<%
    GymBranch branch = (GymBranch) request.getAttribute("branch");
    boolean editing = branch != null;
    String pageTitle = editing ? "Edit Branch" : "Add Branch";
%>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">Branch details</p>
        <a href="<%= request.getContextPath() %>/branches" class="btn btn-outline-secondary">Back to List</a>
    </div>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card">
        <div class="card-body p-4">
            <form method="post" action="<%= request.getContextPath() %>/branches">
                <input type="hidden" name="action" value="<%= editing ? "update" : "add" %>">
                <% if (editing) { %><input type="hidden" name="branchId" value="<%= branch.getBranchId() %>"><% } %>
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Name</label>
                        <input type="text" class="form-control" name="name" placeholder="Enter Name" required
                               value="<%= editing && branch.getName() != null ? branch.getName() : "" %>">
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Address</label>
                        <input type="text" class="form-control" name="address" placeholder="Enter Address"
                               value="<%= editing && branch.getAddress() != null ? branch.getAddress() : "" %>">
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-4 mb-3">
                        <label class="form-label">City</label>
                        <input type="text" class="form-control" name="city" placeholder="Enter City"
                               value="<%= editing && branch.getCity() != null ? branch.getCity() : "" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">State</label>
                        <input type="text" class="form-control" name="state" placeholder="Enter State"
                               value="<%= editing && branch.getState() != null ? branch.getState() : "" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Phone</label>
                        <input type="text" class="form-control" name="phone" placeholder="Enter Phone"
                               value="<%= editing && branch.getPhone() != null ? branch.getPhone() : "" %>">
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control" name="email" placeholder="Enter Email"
                               value="<%= editing && branch.getEmail() != null ? branch.getEmail() : "" %>">
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Status</label>
                        <select class="form-select" name="status">
                            <option value="ACTIVE" <%= editing && branch.isActive() ? "selected" : "" %>>Active</option>
                            <option value="INACTIVE" <%= editing && !branch.isActive() ? "selected" : "" %>>Inactive</option>
                        </select>
                    </div>
                </div>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary"><%= editing ? "Save Changes" : "Add Branch" %></button>
                    <a href="<%= request.getContextPath() %>/branches" class="btn btn-secondary">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
