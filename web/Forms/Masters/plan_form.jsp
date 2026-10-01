<%@ page import="model.MembershipPlan" %>
<%
    MembershipPlan p = (MembershipPlan) request.getAttribute("plan");
    boolean editing = p != null;
    String pageTitle = editing ? "Edit Plan" : "Add Plan";
%>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">Plan details</p>
        <a href="<%= request.getContextPath() %>/plans" class="btn btn-outline-secondary">Back to List</a>
    </div>
</div>
</div>

<div class="row">
<div class="col-12">
    <div class="card">
        <div class="card-body p-4">
            <form method="post" action="<%= request.getContextPath() %>/plans">
                <input type="hidden" name="action" value="<%= editing ? "update" : "add" %>">
                <% if (editing) { %><input type="hidden" name="planId" value="<%= p.getPlanId() %>"><% } %>
                <div class="row">
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Plan Name</label>
                        <input type="text" class="form-control" name="name" placeholder="e.g. 1 Month, 1 Year" required
                               value="<%= editing && p.getName() != null ? p.getName() : "" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Duration (days)</label>
                        <input type="number" min="1" class="form-control" name="durationDays" required
                               value="<%= editing ? p.getDurationDays() : "30" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Price</label>
                        <input type="number" step="0.01" class="form-control" name="price" required
                               value="<%= editing ? p.getPrice() : "0" %>">
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-8 mb-3">
                        <label class="form-label">Description</label>
                        <input type="text" class="form-control" name="description"
                               value="<%= editing && p.getDescription() != null ? p.getDescription() : "" %>">
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Status</label>
                        <select class="form-select" name="status">
                            <option value="ACTIVE" <%= !editing || p.isActive() ? "selected" : "" %>>Active</option>
                            <option value="INACTIVE" <%= editing && !p.isActive() ? "selected" : "" %>>Inactive</option>
                        </select>
                    </div>
                </div>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary"><%= editing ? "Save Changes" : "Add Plan" %></button>
                    <a href="<%= request.getContextPath() %>/plans" class="btn btn-secondary">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
