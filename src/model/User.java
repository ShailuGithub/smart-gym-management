package model;

public class User {

    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String STAFF = "STAFF";

    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private Integer branchId;
    private String email;

    // Raw DB-backed permission flags - only meaningful for non-ADMIN roles; ADMIN always has
    // full rights regardless of these (see canCreate()/canEdit()/canDelete()).
    private boolean permCreate;
    private boolean permEdit;
    private boolean permDelete;

    public User() {
    }

    public User(int userId, String username, String fullName, String role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** "Owner/Admin" for ADMIN (the doc's term for it), otherwise the role name as-is. */
    public String getRoleLabel() {
        return ADMIN.equals(role) ? "Owner/Admin" : role;
    }

    /** Null for an ADMIN - they aren't tied to any single branch, they see all of them. */
    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return ADMIN.equals(role);
    }

    public boolean isManager() {
        return MANAGER.equals(role);
    }

    public boolean isStaff() {
        return STAFF.equals(role);
    }

    public boolean isPermCreate() {
        return permCreate;
    }

    public void setPermCreate(boolean permCreate) {
        this.permCreate = permCreate;
    }

    public boolean isPermEdit() {
        return permEdit;
    }

    public void setPermEdit(boolean permEdit) {
        this.permEdit = permEdit;
    }

    public boolean isPermDelete() {
        return permDelete;
    }

    public void setPermDelete(boolean permDelete) {
        this.permDelete = permDelete;
    }

    /** Effective right to create records - always true for ADMIN, otherwise this account's own flag. */
    public boolean canCreate() {
        return isAdmin() || permCreate;
    }

    public boolean canEdit() {
        return isAdmin() || permEdit;
    }

    public boolean canDelete() {
        return isAdmin() || permDelete;
    }

    // Per-module access: Owner/Admin: everything. Manager: everything within their own branch
    // (Members, Plans, Payments, Reminders, Reports). Staff: Members + Payments only.
    // (Branches, Users, Audit Log, Settings remain Admin-only - unchanged, checked directly with
    // isAdmin() in those servlets, not listed here.)

    public boolean canAccessMembers() {
        return isAdmin() || isManager() || isStaff();
    }

    public boolean canAccessPayments() {
        return isAdmin() || isManager() || isStaff();
    }

    public boolean canAccessPlans() {
        return isAdmin() || isManager();
    }

    public boolean canAccessReminders() {
        return isAdmin() || isManager();
    }

    public boolean canAccessReports() {
        return isAdmin() || isManager();
    }
}
