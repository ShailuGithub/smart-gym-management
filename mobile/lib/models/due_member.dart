import 'member.dart';
import 'membership_status.dart';
import 'reminder.dart';

/// Mirrors service.ReminderService.DueMember - one row of GET /api/reminders/due.
class DueMember {
  final Member member;
  final MembershipStatus status;
  final Reminder? lastReminder;

  DueMember({required this.member, required this.status, this.lastReminder});

  factory DueMember.fromJson(Map<String, dynamic> json) {
    return DueMember(
      member: Member.fromJson(json['member'] as Map<String, dynamic>),
      status: MembershipStatus.fromJson(json['status'] as Map<String, dynamic>),
      lastReminder: json['lastReminder'] == null
          ? null
          : Reminder.fromJson(json['lastReminder'] as Map<String, dynamic>),
    );
  }
}
