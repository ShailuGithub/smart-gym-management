<%@ page import="model.User, model.GymBranch, dao.GymBranchDAO, servlet.BranchContext, java.util.List" %><%
    User currentUser = (User) session.getAttribute("user");
    String ctx = request.getContextPath();
    String uri = request.getRequestURI();
    boolean isAdmin = currentUser != null && currentUser.isAdmin();

    List<GymBranch> branchSwitcherList = java.util.Collections.emptyList();
    Integer selectedBranchId = null;
    if (currentUser != null) {
        try {
            if (isAdmin) {
                branchSwitcherList = new GymBranchDAO().findAll();
                selectedBranchId = BranchContext.currentBranchIdOrNull(request);
            } else {
                selectedBranchId = currentUser.getBranchId();
            }
        } catch (Exception e) {
            // Non-fatal - the switcher is a convenience, never block a page over it.
        }
    }
%><!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= pageTitle %> - Smart Gym Management</title>
    <link href="<%= ctx %>/assets/img/favicon.svg" rel="icon" type="image/svg+xml">
    <link rel="stylesheet" href="<%= ctx %>/assets/inapp-css/main.css">
</head>
<body>
<div id="overlay" class="overlay"></div>

<nav id="topbar" class="navbar bg-white border-bottom fixed-top topbar px-3">
    <button id="toggleBtn" class="d-none d-lg-inline-flex btn btn-light btn-icon btn-sm">
        <i class="ti ti-layout-sidebar-left-expand"></i>
    </button>
    <button id="mobileBtn" class="btn btn-light btn-icon btn-sm d-lg-none me-2">
        <i class="ti ti-layout-sidebar-left-expand"></i>
    </button>
    <div>
        <ul class="list-unstyled d-flex align-items-center mb-0 gap-1">
            <% if (isAdmin) { %>
            <li class="dropdown">
                <a class="btn btn-light btn-sm d-flex align-items-center gap-1" data-bs-toggle="dropdown" href="#" role="button">
                    <i class="ti ti-building"></i>
                    <span class="d-none d-md-inline">
                        <% if (selectedBranchId == null) { %>All Branches<% } else {
                            for (GymBranch b : branchSwitcherList) {
                                if (b.getBranchId() == selectedBranchId) { %><%= b.getName() %><% break; } } } %>
                    </span>
                </a>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><a class="dropdown-item<%= selectedBranchId == null ? " active" : "" %>" href="?branchId=all">All Branches</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <% for (GymBranch b : branchSwitcherList) { %>
                    <li><a class="dropdown-item<%= selectedBranchId != null && b.getBranchId() == selectedBranchId ? " active" : "" %>"
                           href="?branchId=<%= b.getBranchId() %>"><%= b.getName() %></a></li>
                    <% } %>
                </ul>
            </li>
            <% } %>
            <li class="ms-3 dropdown">
                <a href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false" class="d-flex align-items-center gap-2">
                    <span class="avatar avatar-sm rounded-circle bg-primary bg-opacity-10 text-primary d-flex align-items-center justify-content-center">
                        <i class="ti ti-user"></i>
                    </span>
                </a>
                <div class="dropdown-menu dropdown-menu-end p-0" style="min-width: 200px;">
                    <div class="d-flex gap-3 align-items-center border-dashed border-bottom px-3 py-3">
                        <span class="avatar avatar-md rounded-circle bg-primary bg-opacity-10 text-primary d-flex align-items-center justify-content-center">
                            <i class="ti ti-user fs-4"></i>
                        </span>
                        <div>
                            <h4 class="mb-0 small"><%= currentUser != null ? currentUser.getFullName() : "" %></h4>
                            <p class="mb-0 small"><%= currentUser != null ? currentUser.getRoleLabel() : "" %></p>
                        </div>
                    </div>
                    <div class="p-3 d-flex flex-column gap-1 small lh-lg">
                        <a href="<%= ctx %>/change-password"><i class="ti ti-key me-1"></i> Change Password</a>
                        <a href="<%= ctx %>/logout"><i class="ti ti-logout me-1"></i> Logout</a>
                    </div>
                </div>
            </li>
        </ul>
    </div>
</nav>

<aside id="sidebar" class="sidebar">
    <div class="logo-area">
        <a href="<%= ctx %>/dashboard.jsp" class="d-inline-flex align-items-center text-decoration-none">
            <i class="ti ti-barbell fs-4 text-primary"></i>
            <span class="logo-text ms-2 fw-bold">Smart Gym</span>
        </a>
    </div>
    <ul class="nav flex-column">
        <li class="px-4 py-2"><small class="nav-text">Main</small></li>
        <li><a class="nav-link<%= uri.contains("/dashboard.jsp") ? " active" : "" %>" href="<%= ctx %>/dashboard.jsp">
            <i class="ti ti-home"></i><span class="nav-text">Dashboard</span></a></li>
        <% if (isAdmin) { %>
        <li><a class="nav-link<%= uri.contains("/branches") ? " active" : "" %>" href="<%= ctx %>/branches">
            <i class="ti ti-building"></i><span class="nav-text">Branches</span></a></li>
        <% } %>
        <% if (currentUser != null && currentUser.canAccessMembers()) { %>
        <li><a class="nav-link<%= uri.contains("/members") ? " active" : "" %>" href="<%= ctx %>/members">
            <i class="ti ti-users"></i><span class="nav-text">Members</span></a></li>
        <% } %>
        <% if (currentUser != null && currentUser.canAccessPlans()) { %>
        <li><a class="nav-link<%= uri.contains("/plans") ? " active" : "" %>" href="<%= ctx %>/plans">
            <i class="ti ti-clipboard-list"></i><span class="nav-text">Membership Plans</span></a></li>
        <% } %>
        <% if (currentUser != null && currentUser.canAccessPayments()) { %>
        <li><a class="nav-link<%= uri.contains("/payments") ? " active" : "" %>" href="<%= ctx %>/payments">
            <i class="ti ti-cash"></i><span class="nav-text">Payments</span></a></li>
        <% } %>
        <% if (currentUser != null && currentUser.canAccessReminders()) { %>
        <li><a class="nav-link<%= uri.contains("/reminders") ? " active" : "" %>" href="<%= ctx %>/reminders">
            <i class="ti ti-bell-ringing"></i><span class="nav-text">Reminders</span></a></li>
        <% } %>
        <% if (currentUser != null && currentUser.canAccessReports()) { %>
        <li><a class="nav-link<%= uri.contains("/reports") ? " active" : "" %>" href="<%= ctx %>/reports">
            <i class="ti ti-chart-bar"></i><span class="nav-text">Reports</span></a></li>
        <% } %>
        <% if (isAdmin) { %>
        <li class="px-4 pt-4 pb-2"><small class="nav-text">Admin</small></li>
        <li><a class="nav-link<%= uri.contains("/users") ? " active" : "" %>" href="<%= ctx %>/users">
            <i class="ti ti-user-cog"></i><span class="nav-text">Users</span></a></li>
        <li><a class="nav-link<%= uri.contains("/audit-log") ? " active" : "" %>" href="<%= ctx %>/audit-log">
            <i class="ti ti-history"></i><span class="nav-text">Audit Log</span></a></li>
        <li><a class="nav-link<%= uri.contains("/settings") ? " active" : "" %>" href="<%= ctx %>/settings">
            <i class="ti ti-settings"></i><span class="nav-text">Settings</span></a></li>
        <% } %>
        <li class="px-4 pt-4 pb-2"><small class="nav-text">Account</small></li>
        <li><a class="nav-link" href="<%= ctx %>/logout"><i class="ti ti-logout"></i><span class="nav-text">Logout</span></a></li>
    </ul>
</aside>

<main id="content" class="content py-10">
<div class="container-fluid">
<div class="row">
<div class="col-12">
<div class="mb-6">
<h1 class="fs-3 mb-1"><%= pageTitle %></h1>
</div>
</div>
</div>
<% if ("1".equals(request.getParameter("permError"))) { %>
<div class="row">
<div class="col-12">
    <div class="alert alert-danger py-2">
        <i class="ti ti-shield-x"></i> You don't have permission to perform that action.
    </div>
</div>
</div>
<% } %>
