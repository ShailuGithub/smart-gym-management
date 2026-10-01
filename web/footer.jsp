<div class="row">
<div class="col-12">
<footer class="text-center py-2 mt-6 text-secondary">
    <p class="mb-0">&copy; <%= java.time.Year.now() %> Smart Gym Management</p>
</footer>
</div>
</div>
</div><!-- /container-fluid -->
</main>

<script src="<%= request.getContextPath() %>/assets/inapp-js/main.js" type="module"></script>
<script>
function togglePasswordVisibility(btn) {
    var input = btn.closest('.input-group') ? btn.closest('.input-group').querySelector('input') : null;
    if (!input) { return; }
    var icon = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        if (icon) { icon.classList.replace('ti-eye', 'ti-eye-off'); }
    } else {
        input.type = 'password';
        if (icon) { icon.classList.replace('ti-eye-off', 'ti-eye'); }
    }
}
</script>
</body>
</html>
