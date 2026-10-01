<%@ page import="java.math.BigDecimal, java.time.LocalDate, java.util.ArrayList, java.util.List, java.util.Map" %>
<%@ page import="dao.GymBranchDAO, dao.UserDAO, dao.MemberDAO, dao.MemberSubscriptionDAO, dao.AppSettingsDAO, dao.PaymentDAO" %>
<%@ page import="model.GymBranch, model.User, model.Member, model.MemberSubscription, model.MembershipStatus, model.AppSettings, servlet.BranchContext" %>
<%
    String pageTitle = "Dashboard";
    User loggedInUser = (User) session.getAttribute("user");

    List<GymBranch> allBranches = new GymBranchDAO().findAll();
%>
<%@ include file="header.jsp" %>

<% if (allBranches.isEmpty()) { %>
<div class="row">
<div class="col-12">
    <div class="alert alert-info">
        Welcome! Get started by
        <% if (loggedInUser.isAdmin()) { %>
            <a href="<%= request.getContextPath() %>/branches">adding your first branch</a>.
        <% } else { %>
            asking your Admin to add a branch and assign you to it.
        <% } %>
    </div>
</div>
</div>
<% } else {
    Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
    List<GymBranch> scope = new ArrayList<GymBranch>();
    if (scopedBranchId == null) {
        scope = allBranches;
    } else {
        for (GymBranch b : allBranches) {
            if (b.getBranchId() == scopedBranchId) { scope.add(b); }
        }
    }

    MemberDAO memberDAO = new MemberDAO();
    MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    PaymentDAO paymentDAO = new PaymentDAO();
    AppSettings settings = new AppSettingsDAO().find();
    int userCount = new UserDAO().findAll().size();

    int activeMemberCount = 0, expiringSoonCount = 0, expiredCount = 0, noPlanCount = 0;
    BigDecimal todayCollection = BigDecimal.ZERO;
    for (GymBranch b : scope) {
        List<Member> activeMembers = memberDAO.findActiveByBranch(b.getBranchId());
        Map<Integer, MemberSubscription> latestByMember = subscriptionDAO.findLatestForBranch(b.getBranchId());
        activeMemberCount += activeMembers.size();
        for (Member m : activeMembers) {
            MembershipStatus status = MembershipStatus.of(m.getMemberId(), latestByMember.get(m.getMemberId()), settings.getReminderDaysBeforeExpiry());
            if (status.isExpiringSoon()) { expiringSoonCount++; }
            else if (status.isExpired()) { expiredCount++; }
            else if (status.hasNoPlan()) { noPlanCount++; }
        }
        todayCollection = todayCollection.add(paymentDAO.totalForBranchAndDate(b.getBranchId(), LocalDate.now()));
    }
%>
<div class="row row-deck row-cards">
    <div class="col-sm-6 col-lg-3">
        <div class="card">
            <div class="card-body">
                <div class="d-flex align-items-center">
                    <span class="avatar avatar-md bg-primary bg-opacity-10 text-primary rounded-2 me-3"><i class="ti ti-users fs-3"></i></span>
                    <div>
                        <div class="text-secondary small">Active Members</div>
                        <div class="fs-2 fw-bold"><%= activeMemberCount %></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <% boolean linkToReminders = loggedInUser.canAccessReminders(); %>
    <div class="col-sm-6 col-lg-3">
        <% if (linkToReminders) { %><a href="<%= request.getContextPath() %>/reminders" class="text-decoration-none"><% } %>
        <div class="card">
            <div class="card-body">
                <div class="d-flex align-items-center">
                    <span class="avatar avatar-md bg-warning bg-opacity-10 text-warning rounded-2 me-3"><i class="ti ti-clock-exclamation fs-3"></i></span>
                    <div>
                        <div class="text-secondary small">Expiring Soon</div>
                        <div class="fs-2 fw-bold text-body"><%= expiringSoonCount %></div>
                    </div>
                </div>
            </div>
        </div>
        <% if (linkToReminders) { %></a><% } %>
    </div>
    <div class="col-sm-6 col-lg-3">
        <% if (linkToReminders) { %><a href="<%= request.getContextPath() %>/reminders" class="text-decoration-none"><% } %>
        <div class="card">
            <div class="card-body">
                <div class="d-flex align-items-center">
                    <span class="avatar avatar-md bg-danger bg-opacity-10 text-danger rounded-2 me-3"><i class="ti ti-alert-circle fs-3"></i></span>
                    <div>
                        <div class="text-secondary small">Expired / No Plan</div>
                        <div class="fs-2 fw-bold text-body"><%= expiredCount + noPlanCount %></div>
                    </div>
                </div>
            </div>
        </div>
        <% if (linkToReminders) { %></a><% } %>
    </div>
    <div class="col-sm-6 col-lg-3">
        <div class="card">
            <div class="card-body">
                <div class="d-flex align-items-center">
                    <span class="avatar avatar-md bg-success bg-opacity-10 text-success rounded-2 me-3"><i class="ti ti-building fs-3"></i></span>
                    <div>
                        <div class="text-secondary small">Branches / Users</div>
                        <div class="fs-2 fw-bold"><%= allBranches.size() %> / <%= userCount %></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="row row-deck row-cards mt-1">
    <div class="col-sm-6 col-lg-3">
        <div class="card">
            <div class="card-body">
                <div class="d-flex align-items-center">
                    <span class="avatar avatar-md bg-info bg-opacity-10 text-info rounded-2 me-3"><i class="ti ti-cash fs-3"></i></span>
                    <div>
                        <div class="text-secondary small">Today's Collection</div>
                        <div class="fs-2 fw-bold">&#8377; <%= todayCollection %></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<% if (loggedInUser.canAccessReports()) { %>
<div class="row mt-4">
<div class="col-12">
    <div class="alert alert-secondary mb-0">
        <a href="<%= request.getContextPath() %>/reports" class="alert-link"><i class="ti ti-chart-bar me-1"></i>View detailed reports</a>
        &mdash; membership due/expiring, revenue, and branch-wise summaries.
    </div>
</div>
</div>
<% } %>
<% } %>

<%@ include file="footer.jsp" %>
