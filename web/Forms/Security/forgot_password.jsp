<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Forgot Password - Smart Gym Management</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="<%= request.getContextPath() %>/assets/img/favicon.svg" rel="icon" type="image/svg+xml">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/inapp-css/main.css">
</head>
<body>
<div class="container d-flex align-items-center justify-content-center min-vh-100">
    <div class="card" style="max-width:420px; width:100%;">
        <div class="card-body p-5">
            <div class="text-center mb-3">
                <span class="avatar avatar-lg rounded-circle bg-primary bg-opacity-10 text-primary d-inline-flex align-items-center justify-content-center mb-3">
                    <i class="ti ti-key fs-2"></i>
                </span>
                <h1 class="card-title mb-1 h5">Forgot Password</h1>
                <p class="text-secondary small mb-0">Enter your username and the email address on your account.</p>
            </div>

            <% if (Boolean.TRUE.equals(request.getAttribute("submitted"))) {
                String resetLink = (String) request.getAttribute("resetLink");
            %>
                <div class="alert alert-success py-2">
                    If that username and email match our records, a password reset link has been issued.
                </div>
                <% if (resetLink != null) { %>
                <div class="alert alert-info py-2">
                    <strong>Dev mode:</strong> this project has no email service configured, so here's the
                    reset link directly (in production this would be emailed instead):<br>
                    <a href="<%= resetLink %>"><%= resetLink %></a>
                </div>
                <% } %>
            <% } else { %>
                <form method="post" action="<%= request.getContextPath() %>/forgot-password">
                    <!-- Honeypot - hidden from real users, left unfilled by them; a bot that
                         blindly fills in every field it finds gives itself away. See PasswordResetServlet. -->
                    <div style="position:absolute; left:-9999px;" aria-hidden="true">
                        <label for="website">Website</label>
                        <input type="text" id="website" name="website" tabindex="-1" autocomplete="off">
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Username</label>
                        <input type="text" class="form-control" name="username" required autofocus>
                    </div>
                    <div class="mb-4">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control" name="email" required>
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Send Reset Link</button>
                </form>
            <% } %>
            <p class="text-center mt-3 mb-0 small">
                <a href="<%= request.getContextPath() %>/login" class="link-primary">Back to Login</a>
            </p>
        </div>
    </div>
</div>
<script src="<%= request.getContextPath() %>/assets/inapp-js/main.js" type="module"></script>
</body>
</html>
