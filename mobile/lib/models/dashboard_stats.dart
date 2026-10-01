/// Mirrors api.DashboardApiServlet.DashboardStats - the body of GET /api/dashboard.
class DashboardStats {
  final int activeMembers;
  final int expiringSoon;
  final int expiredOrNoPlan;
  final double todaysCollection;
  final int branchCount;
  final int userCount;

  DashboardStats({
    required this.activeMembers,
    required this.expiringSoon,
    required this.expiredOrNoPlan,
    required this.todaysCollection,
    required this.branchCount,
    required this.userCount,
  });

  factory DashboardStats.fromJson(Map<String, dynamic> json) {
    return DashboardStats(
      activeMembers: (json['activeMembers'] as num?)?.toInt() ?? 0,
      expiringSoon: (json['expiringSoon'] as num?)?.toInt() ?? 0,
      expiredOrNoPlan: (json['expiredOrNoPlan'] as num?)?.toInt() ?? 0,
      todaysCollection: (json['todaysCollection'] as num?)?.toDouble() ?? 0,
      branchCount: (json['branchCount'] as num?)?.toInt() ?? 0,
      userCount: (json['userCount'] as num?)?.toInt() ?? 0,
    );
  }
}
