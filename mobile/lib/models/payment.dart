/// Mirrors model.Payment (src/model/Payment.java).
class Payment {
  final int paymentId;
  final int branchId;
  final int memberId;
  final int? subscriptionId;
  final DateTime paymentDate;
  final double amount;
  final String? mode;
  final String? referenceNo;
  final String? remarks;
  final String? memberName;

  Payment({
    required this.paymentId,
    required this.branchId,
    required this.memberId,
    this.subscriptionId,
    required this.paymentDate,
    required this.amount,
    this.mode,
    this.referenceNo,
    this.remarks,
    this.memberName,
  });

  factory Payment.fromJson(Map<String, dynamic> json) {
    return Payment(
      paymentId: json['paymentId'] as int,
      branchId: json['branchId'] as int,
      memberId: json['memberId'] as int,
      subscriptionId: json['subscriptionId'] as int?,
      paymentDate: DateTime.parse(json['paymentDate'] as String),
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
      mode: json['mode'] as String?,
      referenceNo: json['referenceNo'] as String?,
      remarks: json['remarks'] as String?,
      memberName: json['memberName'] as String?,
    );
  }
}
