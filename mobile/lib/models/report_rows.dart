import 'member.dart';
import 'membership_status.dart';

/// Mirrors service.ReportService.DueRow - one row of GET /api/reports/due.
class DueRow {
  final String branchName;
  final Member member;
  final MembershipStatus status;
  final DateTime? lastPaymentDate;

  DueRow({required this.branchName, required this.member, required this.status, this.lastPaymentDate});

  factory DueRow.fromJson(Map<String, dynamic> json) {
    return DueRow(
      branchName: json['branchName'] as String? ?? '',
      member: Member.fromJson(json['member'] as Map<String, dynamic>),
      status: MembershipStatus.fromJson(json['status'] as Map<String, dynamic>),
      lastPaymentDate:
          json['lastPaymentDate'] == null ? null : DateTime.parse(json['lastPaymentDate'] as String),
    );
  }
}

/// Mirrors service.ReportService.RevenueRow - one calendar day's total within GET /api/reports/revenue.
class RevenueRow {
  final DateTime date;
  final double amount;

  RevenueRow({required this.date, required this.amount});

  factory RevenueRow.fromJson(Map<String, dynamic> json) {
    return RevenueRow(
      date: DateTime.parse(json['date'] as String),
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
    );
  }
}

/// Mirrors api.ReportApiServlet.RevenueResponse - the full GET /api/reports/revenue body (rows + total).
class RevenueReport {
  final List<RevenueRow> rows;
  final double total;

  RevenueReport({required this.rows, required this.total});

  factory RevenueReport.fromJson(Map<String, dynamic> json) {
    return RevenueReport(
      rows: (json['rows'] as List<dynamic>)
          .map((e) => RevenueRow.fromJson(e as Map<String, dynamic>))
          .toList(),
      total: (json['total'] as num?)?.toDouble() ?? 0,
    );
  }
}

/// Mirrors service.ReportService.BranchSummaryRow - one row of GET /api/reports/branch-summary.
class BranchSummaryRow {
  final String branchName;
  final int active;
  final int expiringSoon;
  final int expired;
  final int noPlan;

  BranchSummaryRow({
    required this.branchName,
    required this.active,
    required this.expiringSoon,
    required this.expired,
    required this.noPlan,
  });

  factory BranchSummaryRow.fromJson(Map<String, dynamic> json) {
    return BranchSummaryRow(
      branchName: json['branchName'] as String? ?? '',
      active: (json['active'] as num?)?.toInt() ?? 0,
      expiringSoon: (json['expiringSoon'] as num?)?.toInt() ?? 0,
      expired: (json['expired'] as num?)?.toInt() ?? 0,
      noPlan: (json['noPlan'] as num?)?.toInt() ?? 0,
    );
  }
}
