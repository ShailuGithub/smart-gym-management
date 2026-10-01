/// Mirrors model.GymBranch (src/model/GymBranch.java).
class GymBranch {
  final int branchId;
  final String name;
  final String? address;
  final String? city;
  final String? state;
  final String? phone;
  final String? email;
  final String status;

  GymBranch({
    required this.branchId,
    required this.name,
    this.address,
    this.city,
    this.state,
    this.phone,
    this.email,
    required this.status,
  });

  factory GymBranch.fromJson(Map<String, dynamic> json) {
    return GymBranch(
      branchId: json['branchId'] as int,
      name: json['name'] as String? ?? '',
      address: json['address'] as String?,
      city: json['city'] as String?,
      state: json['state'] as String?,
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }
}
