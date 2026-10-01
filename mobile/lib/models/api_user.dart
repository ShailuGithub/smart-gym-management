/// Mirrors AuthApiServlet.UserDto (src/api/AuthApiServlet.java) - the shape returned inside
/// /api/auth/login's response, deliberately narrower than the full server-side model.User (no
/// password hash, no permission flags the mobile app doesn't need).
class ApiUser {
  static const admin = 'ADMIN';
  static const manager = 'MANAGER';
  static const staff = 'STAFF';

  final int userId;
  final String username;
  final String fullName;
  final String? email;
  final String role;
  final int? branchId;

  ApiUser({
    required this.userId,
    required this.username,
    required this.fullName,
    this.email,
    required this.role,
    this.branchId,
  });

  factory ApiUser.fromJson(Map<String, dynamic> json) {
    return ApiUser(
      userId: json['userId'] as int,
      username: json['username'] as String,
      fullName: json['fullName'] as String? ?? json['username'] as String,
      email: json['email'] as String?,
      role: json['role'] as String,
      branchId: json['branchId'] as int?,
    );
  }

  Map<String, dynamic> toJson() => {
        'userId': userId,
        'username': username,
        'fullName': fullName,
        'email': email,
        'role': role,
        'branchId': branchId,
      };

  bool get isAdmin => role == admin;
  bool get isManager => role == manager;
  bool get isStaff => role == staff;

  /// Mirrors model.User.canAccessPlans()/canAccessReports() on the server - Admin+Manager only,
  /// same rule for both. Settings is Admin-only there too (see header.jsp's `isAdmin` guard).
  bool get canAccessPlans => isAdmin || isManager;
  bool get canAccessReports => isAdmin || isManager;
}
