import 'member.dart';
import 'member_subscription.dart';
import 'membership_status.dart';
import 'payment.dart';

/// Mirrors api.MemberApiServlet.MemberView - one row of GET /api/members?branchId=.
class MemberView {
  final Member member;
  final MembershipStatus membershipStatus;

  MemberView({required this.member, required this.membershipStatus});

  factory MemberView.fromJson(Map<String, dynamic> json) {
    return MemberView(
      member: Member.fromJson(json['member'] as Map<String, dynamic>),
      membershipStatus:
          MembershipStatus.fromJson(json['membershipStatus'] as Map<String, dynamic>),
    );
  }
}

/// Mirrors api.MemberApiServlet.MemberDetail - the body of GET /api/members/{id}.
class MemberDetail {
  final Member member;
  final MembershipStatus membershipStatus;
  final List<MemberSubscription> subscriptionHistory;
  final List<Payment> paymentHistory;

  MemberDetail({
    required this.member,
    required this.membershipStatus,
    required this.subscriptionHistory,
    required this.paymentHistory,
  });

  factory MemberDetail.fromJson(Map<String, dynamic> json) {
    return MemberDetail(
      member: Member.fromJson(json['member'] as Map<String, dynamic>),
      membershipStatus:
          MembershipStatus.fromJson(json['membershipStatus'] as Map<String, dynamic>),
      subscriptionHistory: (json['subscriptionHistory'] as List<dynamic>? ?? [])
          .map((e) => MemberSubscription.fromJson(e as Map<String, dynamic>))
          .toList(),
      paymentHistory: (json['paymentHistory'] as List<dynamic>? ?? [])
          .map((e) => Payment.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
