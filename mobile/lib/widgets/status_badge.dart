import 'package:flutter/material.dart';

import '../models/membership_status.dart';
import '../models/reminder.dart';

/// Small pill badge - the Flutter equivalent of the web app's
/// `bg-<color>-subtle text-<color>` Bootstrap badge convention (see model.MembershipStatus /
/// model.Reminder's getBadgeColor() on the server, re-derived here from each model's own state).
class StatusBadge extends StatelessWidget {
  final String label;
  final Color color;

  const StatusBadge({super.key, required this.label, required this.color});

  factory StatusBadge.forMembership(MembershipStatus status) {
    return StatusBadge(label: status.label, color: _colorFor(status.state));
  }

  factory StatusBadge.forReminder(String status) {
    return StatusBadge(label: _reminderLabel(status), color: _reminderColor(status));
  }

  static Color _colorFor(String state) {
    switch (state) {
      case MembershipStatus.active:
        return Colors.green;
      case MembershipStatus.expiringSoon:
        return Colors.orange;
      case MembershipStatus.expired:
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  static String _reminderLabel(String status) {
    switch (status) {
      case Reminder.sent:
        return 'Sent';
      case Reminder.failed:
        return 'Failed';
      case Reminder.loggedOnly:
        return 'Logged Only';
      default:
        return status;
    }
  }

  static Color _reminderColor(String status) {
    switch (status) {
      case Reminder.sent:
        return Colors.green;
      case Reminder.failed:
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontWeight: FontWeight.w600, fontSize: 12),
      ),
    );
  }
}
