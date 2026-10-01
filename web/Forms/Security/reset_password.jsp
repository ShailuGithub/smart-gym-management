<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Reset Password - Smart Gym Management</title>
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
                    <i class="ti ti-shield-lock fs-2"></i>
                </span>
                <h1 class="card-title mb-1 h5">Reset Password</h1>
            </div>
            <%
                boolean valid = Boolean.TRUE.equals(request.getAttribute("valid"));
                String error = (String) request.getAttribute("error");
                String token = (String) request.getAttribute("token");
                if (!valid) {
            %>
                <div class="alert alert-danger py-2">
                    This reset link is invalid or has expired. Please request a new one.
                </div>
                <a class="btn btn-outline-primary w-100" href="<%= request.getContextPath() %>/forgot-password">
                    Request New Link
                </a>
            <% } else { %>
                <% if (error != null) { %>
                    <div class="alert alert-danger py-2"><%= error %></div>
                <% } %>
                <form method="post" action="<%= request.getContextPath() %>/reset-password">
                    <input type="hidden" name="token" value="<%= token %>">
                    <div class="mb-3">
                        <label class="form-label">New Password</label>
                        <div class="input-group">
                            <input type="password" class="form-control" name="newPassword" required minlength="6" autofocus>
                            <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility(this)">
                                <i class="ti ti-eye"></i>
                            </button>
                        </div>
                    </div>
                    <div class="mb-4">
                        <label class="form-label">Confirm New Password</label>
                        <div class="input-group">
                            <input type="password" class="form-control" name="confirmPassword" required minlength="6">
                            <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility(this)">
                                <i class="ti ti-eye"></i>
                            </button>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Reset Password</button>
                </form>
            <% } %>
        </div>
    </div>
</div>
<script src="<%= request.getContextPath() %>/assets/inapp-js/main.js" type="module"></script>
<script>
function togglePasswordVisibility(btn) {
    var input = btn.closest('.input-group').querySelector('input');
    var icon = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('ti-eye', 'ti-eye-off');
    } else {
        input.type = 'password';
        icon.classList.replace('ti-eye-off', 'ti-eye');
    }
}
</script>
</body>
</html>
