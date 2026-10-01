/// Mirrors model.MembershipPlan (src/model/MembershipPlan.java).
class MembershipPlan {
  final int planId;
  final String name;
  final int durationDays;
  final double price;
  final String? description;
  final String status;

  MembershipPlan({
    required this.planId,
    required this.name,
    required this.durationDays,
    required this.price,
    this.description,
    required this.status,
  });

  factory MembershipPlan.fromJson(Map<String, dynamic> json) {
    return MembershipPlan(
      planId: json['planId'] as int,
      name: json['name'] as String? ?? '',
      durationDays: (json['durationDays'] as num?)?.toInt() ?? 0,
      price: (json['price'] as num?)?.toDouble() ?? 0,
      description: json['description'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }
}
