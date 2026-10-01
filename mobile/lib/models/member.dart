/// Mirrors model.Member (src/model/Member.java).
class Member {
  static const active = 'ACTIVE';
  static const inactive = 'INACTIVE';

  final int memberId;
  final int branchId;
  final String fullName;
  final String? phone;
  final String? email;
  final String? gender;
  final DateTime? dob;
  final String? address;
  final DateTime? joinDate;
  final String status;

  Member({
    required this.memberId,
    required this.branchId,
    required this.fullName,
    this.phone,
    this.email,
    this.gender,
    this.dob,
    this.address,
    this.joinDate,
    required this.status,
  });

  factory Member.fromJson(Map<String, dynamic> json) {
    return Member(
      memberId: json['memberId'] as int,
      branchId: json['branchId'] as int,
      fullName: json['fullName'] as String? ?? '',
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      gender: json['gender'] as String?,
      dob: json['dob'] == null ? null : DateTime.parse(json['dob'] as String),
      address: json['address'] as String?,
      joinDate: json['joinDate'] == null ? null : DateTime.parse(json['joinDate'] as String),
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  bool get isActive => status == active;
}
