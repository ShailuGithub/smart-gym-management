/// Mirrors model.MemberSubscription (src/model/MemberSubscription.java) - one row per plan
/// purchase/renewal. planName/memberName are only populated by DAO methods that join, per the
/// server-side comment on those fields; treat them as optional here too.
class MemberSubscription {
  static const active = 'ACTIVE';
  static const expired = 'EXPIRED';
  static const cancelled = 'CANCELLED';

  final int subscriptionId;
  final int memberId;
  final int branchId;
  final int planId;
  final DateTime startDate;
  final DateTime endDate;
  final double amountCharged;
  final String status;
  final String? planName;
  final String? memberName;

  MemberSubscription({
    required this.subscriptionId,
    required this.memberId,
    required this.branchId,
    required this.planId,
    required this.startDate,
    required this.endDate,
    required this.amountCharged,
    required this.status,
    this.planName,
    this.memberName,
  });

  factory MemberSubscription.fromJson(Map<String, dynamic> json) {
    return MemberSubscription(
      subscriptionId: json['subscriptionId'] as int,
      memberId: json['memberId'] as int,
      branchId: json['branchId'] as int,
      planId: json['planId'] as int,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      amountCharged: (json['amountCharged'] as num?)?.toDouble() ?? 0,
      status: json['status'] as String? ?? 'ACTIVE',
      planName: json['planName'] as String?,
      memberName: json['memberName'] as String?,
    );
  }
}
