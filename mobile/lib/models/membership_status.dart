import 'member_subscription.dart';

/// Mirrors model.MembershipStatus on the server (src/model/MembershipStatus.java) - the API
/// sends the raw fields (memberId, currentSubscription, state, daysRemaining) since Gson only
/// serializes fields, not methods, so the label/badge-color logic there (getLabel()/
/// getBadgeColor()) is re-derived here from `state` to match the web app's badges exactly.
class MembershipStatus {
  static const active = 'ACTIVE';
  static const expiringSoon = 'EXPIRING_SOON';
  static const expired = 'EXPIRED';
  static const noPlan = 'NO_PLAN';

  final int memberId;
  final MemberSubscription? currentSubscription;
  final String state;
  final int daysRemaining;

  MembershipStatus({
    required this.memberId,
    required this.currentSubscription,
    required this.state,
    required this.daysRemaining,
  });

  factory MembershipStatus.fromJson(Map<String, dynamic> json) {
    return MembershipStatus(
      memberId: json['memberId'] as int,
      currentSubscription: json['currentSubscription'] == null
          ? null
          : MemberSubscription.fromJson(
              json['currentSubscription'] as Map<String, dynamic>),
      state: json['state'] as String,
      daysRemaining: (json['daysRemaining'] as num).toInt(),
    );
  }

  bool get isActive => state == active;
  bool get isExpiringSoon => state == expiringSoon;
  bool get isExpired => state == expired;
  bool get hasNoPlan => state == noPlan;

  String get label {
    switch (state) {
      case active:
        return 'Active';
      case expiringSoon:
        return 'Expiring Soon';
      case expired:
        return 'Expired';
      default:
        return 'No Plan';
    }
  }
}
