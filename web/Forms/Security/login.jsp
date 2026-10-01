<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Sign in - Smart Gym Management</title>
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
                    <i class="ti ti-barbell fs-2"></i>
                </span>
                <h1 class="card-title mb-1 h5">Smart Gym Management</h1>
                <p class="text-secondary small mb-0">Sign in to your account</p>
            </div>

            <%
                String error = (String) request.getAttribute("error");
                if (error == null) {
                    error = request.getParameter("error");
                }
                if (error != null) {
            %>
                <div class="alert alert-danger py-2"><%= error %></div>
            <% } %>
            <% if ("1".equals(request.getParameter("reset"))) { %>
                <div class="alert alert-success py-2">Password reset. Please log in with your new password.</div>
            <% } %>

            <form method="post" action="<%= request.getContextPath() %>/login" class="mt-3">
                <div class="mb-3">
                    <label for="username" class="form-label">Username</label>
                    <input id="username" name="username" type="text" class="form-control" placeholder="Username" required autofocus>
                </div>

                <div class="mb-3">
                    <label for="password" class="form-label d-flex justify-content-between">
                        <span>Password</span>
                        <a href="<%= request.getContextPath() %>/forgot-password" class="small link-primary">Forgot Password?</a>
                    </label>
                    <div class="input-group">
                        <input id="password" name="password" type="password" class="form-control" placeholder="Password" required>
                        <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility(this)">
                            <i class="ti ti-eye"></i>
                        </button>
                    </div>
                </div>

                <button class="btn btn-primary w-100" type="submit">Sign in</button>
            </form>

            <p class="text-center text-secondary small mt-4 mb-0">
                &copy; <%= java.time.Year.now() %> Smart Gym Management
            </p>
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
