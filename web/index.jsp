<%@ page import="model.User" %><%
    User loggedInUser = (User) session.getAttribute("user");
    if (loggedInUser != null) {
        response.sendRedirect(request.getContextPath() + "/dashboard.jsp");
        return;
    }
    String ctx = request.getContextPath();
%><!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Smart Gym Management</title>
    <link href="<%= ctx %>/assets/img/favicon.svg" rel="icon" type="image/svg+xml">
    <link rel="stylesheet" href="<%= ctx %>/assets/inapp-css/main.css">
    <style>
        .home-header {
            position: fixed; top: 0; left: 0; right: 0; z-index: 20;
            background: rgba(255,255,255,0.95); backdrop-filter: blur(6px);
            padding: 14px 0; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
        }
        .home-hero { padding: 160px 0 80px; }
        .home-footer { padding: 24px 0; text-align: center; }
    </style>
</head>
<body>

<header class="home-header">
    <div class="container d-flex align-items-center justify-content-between">
        <a href="<%= ctx %>/" class="d-flex align-items-center text-decoration-none">
            <span class="avatar avatar-sm rounded-circle bg-primary bg-opacity-10 text-primary d-flex align-items-center justify-content-center">
                <i class="ti ti-barbell"></i>
            </span>
            <span class="ms-2 fw-bold text-body">Smart Gym Management</span>
        </a>
        <button type="button" class="btn btn-primary rounded-pill px-4" data-bs-toggle="modal" data-bs-target="#homeLoginModal">
            Login
        </button>
    </div>
</header>

<section class="home-hero">
    <div class="container text-center">
        <span class="avatar avatar-xl rounded-circle bg-primary bg-opacity-10 text-primary d-inline-flex align-items-center justify-content-center mb-4">
            <i class="ti ti-barbell fs-1"></i>
        </span>
        <h1 class="mb-3 fw-bold">Run every gym you own, from one place</h1>
        <p class="mb-4 mx-auto text-secondary" style="max-width:620px;">
            Members, memberships, dues, payments, and renewal reminders - tracked across every branch, in real time.
        </p>
        <button type="button" class="btn btn-primary rounded-pill px-4 py-2" data-bs-toggle="modal" data-bs-target="#homeLoginModal">
            Login to Dashboard
        </button>
    </div>
</section>

<section class="py-10 bg-light">
    <div class="container">
        <div class="text-center mb-5">
            <h2 class="fw-bold">Everything a gym business needs</h2>
        </div>
        <div class="row g-4">
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <div class="icon-shape icon-md bg-primary bg-opacity-10 text-primary rounded-2 mb-3">
                        <i class="ti ti-building fs-4"></i>
                    </div>
                    <h5>Multiple Branches</h5>
                    <p class="text-secondary mb-0 small">Manage every location from one login, switch between them instantly.</p>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <div class="icon-shape icon-md bg-success bg-opacity-10 text-success rounded-2 mb-3">
                        <i class="ti ti-users fs-4"></i>
                    </div>
                    <h5>Member Management</h5>
                    <p class="text-secondary mb-0 small">Add members, track renewals, and manage profiles in a few clicks.</p>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <div class="icon-shape icon-md bg-warning bg-opacity-10 text-warning rounded-2 mb-3">
                        <i class="ti ti-report-money fs-4"></i>
                    </div>
                    <h5>Dues &amp; Payments</h5>
                    <p class="text-secondary mb-0 small">See who's due and who's paid, at a glance, every month.</p>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <div class="icon-shape icon-md bg-info bg-opacity-10 text-info rounded-2 mb-3">
                        <i class="ti ti-bell-ringing fs-4"></i>
                    </div>
                    <h5>Payment Reminders</h5>
                    <p class="text-secondary mb-0 small">Automate reminders for due and expiring memberships.</p>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <div class="icon-shape icon-md bg-danger bg-opacity-10 text-danger rounded-2 mb-3">
                        <i class="ti ti-chart-bar fs-4"></i>
                    </div>
                    <h5>Reports &amp; Insights</h5>
                    <p class="text-secondary mb-0 small">Real-time reports on revenue, dues, and membership activity.</p>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <div class="icon-shape icon-md bg-secondary bg-opacity-10 text-secondary rounded-2 mb-3">
                        <i class="ti ti-shield-check fs-4"></i>
                    </div>
                    <h5>Role-Based Access</h5>
                    <p class="text-secondary mb-0 small">Give managers and staff exactly the access they need, nothing more.</p>
                </div>
            </div>
        </div>
    </div>
</section>

<footer class="home-footer text-secondary small">
    &copy; <%= java.time.Year.now() %> Smart Gym Management
</footer>

<div class="modal fade" id="homeLoginModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Login</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <%
                    String error = request.getParameter("error");
                    if (error != null) {
                %>
                    <div class="alert alert-danger py-2"><%= error %></div>
                <% } %>
                <form method="post" action="<%= ctx %>/login" id="homeLoginForm">
                    <div class="mb-3">
                        <label class="form-label">Username</label>
                        <input type="text" name="username" class="form-control" placeholder="Username" autocomplete="off" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Password</label>
                        <input type="password" name="password" class="form-control" placeholder="Password" required>
                    </div>
                </form>
                <p class="text-center mb-0 small">
                    <a href="<%= ctx %>/forgot-password" class="link-primary">Forgot password?</a>
                </p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" form="homeLoginForm" class="btn btn-primary">Login</button>
            </div>
        </div>
    </div>
</div>

<script src="<%= ctx %>/assets/inapp-js/main.js" type="module"></script>
</body>
</html>
