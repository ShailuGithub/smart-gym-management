<%@ page import="java.util.List, model.Member" %>
<%
    String pageTitle = "Record Payment";
    List<Member> activeMembers = (List<Member>) request.getAttribute("activeMembers");
    String targetBranchName = (String) request.getAttribute("targetBranchName");
    String preselectMemberId = (String) request.getAttribute("preselectMemberId");
%>
<%@ include file="../../header.jsp" %>

<div class="row">
<div class="col-md-8 col-lg-6">
    <div class="card">
        <div class="card-body">
            <h3 class="card-title">Record Payment</h3>
            <p class="text-secondary">
                <span class="badge bg-primary-subtle text-primary"><i class="ti ti-building me-1"></i><%= targetBranchName %></span>
            </p>
            <form method="post" action="<%= request.getContextPath() %>/payments">
                <input type="hidden" name="action" value="add">
                <div class="mb-3">
                    <label class="form-label">Member</label>
                    <select class="form-select" name="memberId" required>
                        <option value="">-- select member --</option>
                        <% for (Member m : activeMembers) { %>
                        <option value="<%= m.getMemberId() %>" <%= m.getMemberId() == (preselectMemberId == null ? -1 : Integer.parseInt(preselectMemberId)) ? "selected" : "" %>>
                            <%= m.getFullName() %><%= m.getPhone() == null ? "" : " (" + m.getPhone() + ")" %>
                        </option>
                        <% } %>
                    </select>
                </div>
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Payment Date</label>
                        <input type="date" class="form-control" name="paymentDate" value="<%= java.time.LocalDate.now() %>">
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Amount</label>
                        <input type="number" step="0.01" class="form-control" name="amount" required>
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Mode</label>
                        <select class="form-select" name="mode">
                            <option value="CASH">Cash</option>
                            <option value="UPI">UPI</option>
                            <option value="BANK_TRANSFER">Bank Transfer</option>
                            <option value="CARD">Card</option>
                            <option value="CHEQUE">Cheque</option>
                        </select>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Reference No (optional)</label>
                        <input type="text" class="form-control" name="referenceNo">
                    </div>
                </div>
                <div class="mb-3">
                    <label class="form-label">Remarks (optional)</label>
                    <textarea class="form-control" name="remarks" rows="2"></textarea>
                </div>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary">Save Payment</button>
                    <a href="<%= request.getContextPath() %>/payments" class="btn btn-outline-secondary">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</div>
</div>

<%@ include file="../../footer.jsp" %>
