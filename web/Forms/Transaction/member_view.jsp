<%@ page import="java.util.List, model.Member, model.MemberSubscription, model.MembershipStatus, model.MembershipPlan, model.Payment, model.User" %>
<%
    Member m = (Member) request.getAttribute("member");
    String targetBranchName = (String) request.getAttribute("targetBranchName");
    List<MemberSubscription> history = (List<MemberSubscription>) request.getAttribute("subscriptionHistory");
    MembershipStatus status = (MembershipStatus) request.getAttribute("membershipStatus");
    List<MembershipPlan> activePlans = (List<MembershipPlan>) request.getAttribute("activePlans");
    List<Payment> paymentHistory = (List<Payment>) request.getAttribute("paymentHistory");
    User loggedInUser = (User) session.getAttribute("user");
    String pageTitle = "Member: " + m.getFullName();
%>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-12">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <p class="mb-0 text-secondary">
            <span class="badge bg-primary-subtle text-primary"><i class="ti ti-building me-1"></i><%= targetBranchName %></span>
        </p>
        <a href="<%= request.getContextPath() %>/members" class="btn btn-outline-secondary">Back to List</a>
    </div>
</div>
</div>

<div class="row row-deck row-cards mb-2">
    <div class="col-md-6">
        <div class="card">
            <div class="card-body">
                <h3 class="card-title">Member Details</h3>
                <dl class="row mb-0">
                    <dt class="col-5">Phone</dt><dd class="col-7"><%= m.getPhone() == null ? "-" : m.getPhone() %></dd>
                    <dt class="col-5">Email</dt><dd class="col-7"><%= m.getEmail() == null ? "-" : m.getEmail() %></dd>
                    <dt class="col-5">Gender</dt><dd class="col-7"><%= m.getGender() == null ? "-" : m.getGender() %></dd>
                    <dt class="col-5">Date of Birth</dt><dd class="col-7"><%= m.getDob() == null ? "-" : m.getDob() %></dd>
                    <dt class="col-5">Address</dt><dd class="col-7"><%= m.getAddress() == null ? "-" : m.getAddress() %></dd>
                    <dt class="col-5">Joined</dt><dd class="col-7"><%= m.getJoinDate() %></dd>
                    <dt class="col-5">Status</dt>
                    <dd class="col-7"><span class="badge bg-<%= m.isActive() ? "success" : "secondary" %>-subtle text-<%= m.isActive() ? "success" : "secondary" %>"><%= m.getStatus() %></span></dd>
                </dl>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card">
            <div class="card-body">
                <h3 class="card-title">Current Membership</h3>
                <p>
                    <span class="badge bg-<%= status.getBadgeColor() %>-subtle text-<%= status.getBadgeColor() %> fs-5"><%= status.getLabel() %></span>
                </p>
                <% if (status.getCurrentSubscription() != null) { %>
                <dl class="row mb-3">
                    <dt class="col-5">Plan</dt><dd class="col-7"><%= status.getCurrentSubscription().getPlanName() %></dd>
                    <dt class="col-5">Valid From</dt><dd class="col-7"><%= status.getCurrentSubscription().getStartDate() %></dd>
                    <dt class="col-5">Valid Till</dt><dd class="col-7"><%= status.getCurrentSubscription().getEndDate() %></dd>
                    <dt class="col-5">Amount Charged</dt><dd class="col-7">&#8377; <%= status.getCurrentSubscription().getAmountCharged() %></dd>
                </dl>
                <% } else { %>
                <p class="text-secondary">This member has never been assigned a plan.</p>
                <% } %>

                <% if (loggedInUser.canEdit()) { %>
                <form method="post" action="<%= request.getContextPath() %>/subscriptions" class="border-top pt-3">
                    <input type="hidden" name="memberId" value="<%= m.getMemberId() %>">
                    <div class="row g-2 align-items-end">
                        <div class="col-md-4">
                            <label class="form-label">Plan</label>
                            <select class="form-select" name="planId" required>
                                <% for (MembershipPlan p : activePlans) { %>
                                <option value="<%= p.getPlanId() %>"><%= p.getName() %> (&#8377; <%= p.getPrice() %>, <%= p.getDurationDays() %>d)</option>
                                <% } %>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Amount (optional)</label>
                            <input type="number" step="0.01" class="form-control" name="amount" placeholder="Plan price">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Payment Mode</label>
                            <select class="form-select" name="mode">
                                <option value="CASH">Cash</option>
                                <option value="UPI">UPI</option>
                                <option value="BANK_TRANSFER">Bank Transfer</option>
                                <option value="CARD">Card</option>
                                <option value="CHEQUE">Cheque</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <button type="submit" class="btn btn-primary w-100">
                                <%= status.getCurrentSubscription() == null ? "Assign" : "Renew" %>
                            </button>
                        </div>
                    </div>
                    <div class="form-text mt-1">This also records a matching entry in the member's payment history below.</div>
                </form>
                <% } %>
            </div>
        </div>
    </div>
</div>

<div class="row">
<div class="col-12">
    <div class="card table-responsive">
        <div class="card-header"><h3 class="card-title mb-0">Membership History</h3></div>
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light">
                <tr><th>Plan</th><th>Start</th><th>End</th><th>Amount</th><th>Status</th></tr>
            </thead>
            <tbody>
            <%
                if (history.isEmpty()) {
            %>
                <tr><td colspan="5" class="text-secondary">No membership history yet.</td></tr>
            <%
                }
                for (MemberSubscription s : history) {
            %>
                <tr>
                    <td><%= s.getPlanName() %></td>
                    <td><%= s.getStartDate() %></td>
                    <td><%= s.getEndDate() %></td>
                    <td>&#8377; <%= s.getAmountCharged() %></td>
                    <td><span class="badge bg-<%= s.isActive() ? "success" : "secondary" %>-subtle text-<%= s.isActive() ? "success" : "secondary" %>"><%= s.getStatus() %></span></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
</div>

<div class="row mt-4">
<div class="col-12">
    <div class="card table-responsive">
        <div class="card-header d-flex justify-content-between align-items-center">
            <h3 class="card-title mb-0">Payment History</h3>
            <% if (loggedInUser.canCreate()) { %>
            <a href="<%= request.getContextPath() %>/payments?view=form&memberId=<%= m.getMemberId() %>" class="btn btn-sm btn-outline-primary">
                <i class="ti ti-plus me-1"></i> Record Payment
            </a>
            <% } %>
        </div>
        <table class="table mb-0 text-nowrap table-hover">
            <thead class="table-light border-light">
                <tr><th>Date</th><th>Amount</th><th>Mode</th><th>Reference</th><th>Remarks</th></tr>
            </thead>
            <tbody>
            <%
                if (paymentHistory == null || paymentHistory.isEmpty()) {
            %>
                <tr><td colspan="5" class="text-secondary">No payments recorded yet.</td></tr>
            <%
                } else {
                for (Payment p : paymentHistory) {
            %>
                <tr>
                    <td><%= p.getPaymentDate() %></td>
                    <td>&#8377; <%= p.getAmount() %></td>
                    <td><%= p.getMode() == null ? "-" : p.getMode() %></td>
                    <td><%= p.getReferenceNo() == null ? "-" : p.getReferenceNo() %></td>
                    <td><%= p.getRemarks() == null ? "-" : p.getRemarks() %></td>
                </tr>
            <%
                }
                }
            %>
            </tbody>
        </table>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
