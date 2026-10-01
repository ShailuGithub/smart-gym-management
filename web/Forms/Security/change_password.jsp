<% String pageTitle = "Change Password"; %>
<%@ include file="../../header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card">
            <div class="card-body p-4">
                <h5 class="card-title mb-3">Change Password</h5>
                <%
                    if ("1".equals(request.getParameter("saved"))) {
                %>
                    <div class="alert alert-success py-2">Password changed successfully.</div>
                <% }
                    String error = (String) request.getAttribute("error");
                    if (error != null) {
                %>
                    <div class="alert alert-danger py-2"><%= error %></div>
                <% } %>
                <form method="post" action="<%= request.getContextPath() %>/change-password">
                    <div class="mb-3">
                        <label class="form-label">Current Password</label>
                        <div class="input-group">
                            <input type="password" class="form-control" name="currentPassword" required>
                            <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility(this)">
                                <i class="ti ti-eye"></i>
                            </button>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">New Password</label>
                        <div class="input-group">
                            <input type="password" class="form-control" name="newPassword" minlength="6" required>
                            <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility(this)">
                                <i class="ti ti-eye"></i>
                            </button>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Confirm New Password</label>
                        <div class="input-group">
                            <input type="password" class="form-control" name="confirmPassword" minlength="6" required>
                            <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility(this)">
                                <i class="ti ti-eye"></i>
                            </button>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary">Change Password</button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../../footer.jsp" %>
