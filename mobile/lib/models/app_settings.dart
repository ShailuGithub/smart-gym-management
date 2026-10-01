/// Mirrors model.AppSettings (src/model/AppSettings.java) - the singleton business-wide
/// settings row served by api.SettingsApiServlet.
class AppSettings {
  final String? businessName;
  final String? address;
  final String? phone;
  final String? email;
  final String currencySymbol;
  final int reminderDaysBeforeExpiry;
  final bool smsEnabled;

  AppSettings({
    this.businessName,
    this.address,
    this.phone,
    this.email,
    required this.currencySymbol,
    required this.reminderDaysBeforeExpiry,
    required this.smsEnabled,
  });

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    return AppSettings(
      businessName: json['businessName'] as String?,
      address: json['address'] as String?,
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      currencySymbol: json['currencySymbol'] as String? ?? 'Rs.',
      reminderDaysBeforeExpiry: (json['reminderDaysBeforeExpiry'] as num?)?.toInt() ?? 3,
      smsEnabled: json['smsEnabled'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() => {
        'businessName': businessName,
        'address': address,
        'phone': phone,
        'email': email,
        'currencySymbol': currencySymbol,
        'reminderDaysBeforeExpiry': reminderDaysBeforeExpiry,
        'smsEnabled': smsEnabled,
      };
}
