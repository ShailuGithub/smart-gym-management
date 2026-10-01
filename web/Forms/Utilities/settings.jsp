<%@ page import="model.AppSettings" %>
<% String pageTitle = "Settings"; %>
<%@ include file="../../header.jsp" %>

<%
    AppSettings settings = (AppSettings) request.getAttribute("settings");
%>

<% if ("1".equals(request.getParameter("saved"))) { %>
<div class="row"><div class="col-12"><div class="alert alert-success py-2">Settings saved.</div></div></div>
<% } %>

<div class="row">
<div class="col-12">
    <div class="card">
        <div class="card-body p-4">
            <form method="post" action="<%= request.getContextPath() %>/settings">
                <div class="row">
                    <div class="col-12 col-md-4 mb-3">
                        <label class="form-label">Business Name</label>
                        <input type="text" class="form-control" name="businessName" value="<%= settings.getBusinessName() == null ? "" : settings.getBusinessName() %>" required>
                    </div>
                    <div class="col-12 col-md-4 mb-3">
                        <label class="form-label">Address</label>
                        <input type="text" class="form-control" name="address" value="<%= settings.getAddress() == null ? "" : settings.getAddress() %>">
                    </div>
                    <div class="col-12 col-md-2 mb-3">
                        <label class="form-label">Phone</label>
                        <input type="text" class="form-control" name="phone" value="<%= settings.getPhone() == null ? "" : settings.getPhone() %>">
                    </div>
                    <div class="col-12 col-md-2 mb-3">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control" name="email" value="<%= settings.getEmail() == null ? "" : settings.getEmail() %>">
                    </div>
                </div>
                <div class="row">
                    <div class="col-12 col-md-3 mb-3">
                        <label class="form-label">Currency Symbol</label>
                        <input type="text" class="form-control" name="currencySymbol" value="<%= settings.getCurrencySymbol() == null ? "Rs." : settings.getCurrencySymbol() %>">
                    </div>
                    <div class="col-12 col-md-4 mb-3">
                        <label class="form-label">Reminder Days Before Expiry</label>
                        <input type="number" min="0" max="30" class="form-control" name="reminderDaysBeforeExpiry" value="<%= settings.getReminderDaysBeforeExpiry() %>">
                        <div class="form-text">How many days before a membership expires to flag it as "expiring soon".</div>
                    </div>
                    <div class="col-12 col-md-3 mb-3 d-flex align-items-end">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" name="smsEnabled" id="smsEnabled" <%= settings.isSmsEnabled() ? "checked" : "" %>>
                            <label class="form-check-label" for="smsEnabled">
                                Send SMS reminders
                                <div class="form-text mb-0">Unchecked = reminders are only logged, never actually sent.</div>
                            </label>
                        </div>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary">Save</button>
            </form>
        </div>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
