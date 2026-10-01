<%@ page import="model.Member" %>
<%
    Member m = (Member) request.getAttribute("member");
    boolean editing = m != null;
    String targetBranchName = (String) request.getAttribute("targetBranchName");
    String pageTitle = editing ? "Edit Member" : "Add Member";
%>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">
            Member details
            <% if (targetBranchName != null) { %>
                &mdash; <span class="badge bg-primary-subtle text-primary"><i class="ti ti-building me-1"></i><%= targetBranchName %></span>
            <% } %>
        </p>
        <a href="<%= request.getContextPath() %>/members" class="btn btn-outline-secondary">Back to List</a>
    </div>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card">
        <div class="card-body p-4">
            <form method="post" action="<%= request.getContextPath() %>/members">
                <input type="hidden" name="action" value="<%= editing ? "update" : "add" %>">
                <% if (editing) { %><input type="hidden" name="memberId" value="<%= m.getMemberId() %>"><% } %>
                <div class="row">
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Full Name</label>
                        <input type="text" class="form-control" name="fullName" placeholder="Enter Name" required
                               value="<%= editing && m.getFullName() != null ? m.getFullName() : "" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Phone</label>
                        <input type="text" class="form-control" name="phone" placeholder="Enter Phone"
                               value="<%= editing && m.getPhone() != null ? m.getPhone() : "" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control" name="email" placeholder="Enter Email"
                               value="<%= editing && m.getEmail() != null ? m.getEmail() : "" %>">
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Gender</label>
                        <select class="form-select" name="gender">
                            <option value="">(not set)</option>
                            <option value="Male" <%= editing && "Male".equals(m.getGender()) ? "selected" : "" %>>Male</option>
                            <option value="Female" <%= editing && "Female".equals(m.getGender()) ? "selected" : "" %>>Female</option>
                            <option value="Other" <%= editing && "Other".equals(m.getGender()) ? "selected" : "" %>>Other</option>
                        </select>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Date of Birth</label>
                        <input type="date" class="form-control" name="dob"
                               value="<%= editing && m.getDob() != null ? m.getDob() : "" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Join Date</label>
                        <input type="date" class="form-control" name="joinDate"
                               value="<%= editing && m.getJoinDate() != null ? m.getJoinDate() : java.time.LocalDate.now() %>">
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-12 mb-3">
                        <label class="form-label">Address</label>
                        <input type="text" class="form-control" name="address"
                               value="<%= editing && m.getAddress() != null ? m.getAddress() : "" %>">
                    </div>
                </div>
                <% if (editing) { %>
                <div class="row">
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Status</label>
                        <select class="form-select" name="status">
                            <option value="ACTIVE" <%= m.isActive() ? "selected" : "" %>>Active</option>
                            <option value="INACTIVE" <%= !m.isActive() ? "selected" : "" %>>Inactive</option>
                        </select>
                    </div>
                </div>
                <% } %>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary"><%= editing ? "Save Changes" : "Add Member" %></button>
                    <a href="<%= request.getContextPath() %>/members" class="btn btn-secondary">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
