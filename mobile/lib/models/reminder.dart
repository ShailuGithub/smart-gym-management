/// Mirrors model.Reminder (src/model/Reminder.java).
class Reminder {
  static const expiry = 'EXPIRY';
  static const due = 'DUE';
  static const sent = 'SENT';
  static const failed = 'FAILED';
  static const loggedOnly = 'LOGGED_ONLY';

  final int reminderId;
  final int memberId;
  final int branchId;
  final String type;
  final String channel;
  final String? message;
  final String status;
  final DateTime? sentAt;

  Reminder({
    required this.reminderId,
    required this.memberId,
    required this.branchId,
    required this.type,
    required this.channel,
    this.message,
    required this.status,
    this.sentAt,
  });

  factory Reminder.fromJson(Map<String, dynamic> json) {
    return Reminder(
      reminderId: json['reminderId'] as int,
      memberId: json['memberId'] as int,
      branchId: json['branchId'] as int,
      type: json['type'] as String? ?? Reminder.due,
      channel: json['channel'] as String? ?? 'SMS',
      message: json['message'] as String?,
      status: json['status'] as String? ?? '',
      sentAt: json['sentAt'] == null ? null : DateTime.parse(json['sentAt'] as String),
    );
  }
}
