import 'package:flutter/material.dart';

import '../models/app_settings.dart';
import '../services/api_client.dart';

/// Business-wide settings (GET/PUT /api/settings) - entry to this screen is already Admin-only
/// from the drawer (mirrors header.jsp's `isAdmin` guard on the web Settings nav item), and the
/// server also enforces that on PUT (see api.SettingsApiServlet), so this screen doesn't need its
/// own extra role check to be safe.
class SettingsScreen extends StatefulWidget {
  final ApiClient apiClient;

  const SettingsScreen({super.key, required this.apiClient});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _formKey = GlobalKey<FormState>();
  final _businessNameController = TextEditingController();
  final _addressController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  final _currencyController = TextEditingController();
  final _reminderDaysController = TextEditingController();
  bool _smsEnabled = false;

  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _businessNameController.dispose();
    _addressController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    _currencyController.dispose();
    _reminderDaysController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final json = await widget.apiClient.get('/settings') as Map<String, dynamic>;
      final s = AppSettings.fromJson(json);
      _businessNameController.text = s.businessName ?? '';
      _addressController.text = s.address ?? '';
      _phoneController.text = s.phone ?? '';
      _emailController.text = s.email ?? '';
      _currencyController.text = s.currencySymbol;
      _reminderDaysController.text = s.reminderDaysBeforeExpiry.toString();
      setState(() => _smsEnabled = s.smsEnabled);
    } catch (e) {
      setState(() => _error = 'Failed to load settings: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await widget.apiClient.put('/settings', body: {
        'businessName': _businessNameController.text.trim(),
        'address': _addressController.text.trim(),
        'phone': _phoneController.text.trim(),
        'email': _emailController.text.trim(),
        'currencySymbol': _currencyController.text.trim(),
        'reminderDaysBeforeExpiry': int.parse(_reminderDaysController.text.trim()),
        'smsEnabled': _smsEnabled,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Settings saved.')));
      }
    } catch (e) {
      setState(() => _error = 'Failed to save: $e');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _businessNameController.text.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(_error!, textAlign: TextAlign.center),
                        const SizedBox(height: 16),
                        FilledButton(onPressed: _load, child: const Text('Retry')),
                      ],
                    ),
                  ),
                )
              : Form(
                  key: _formKey,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      TextFormField(
                        controller: _businessNameController,
                        decoration: const InputDecoration(labelText: 'Business Name', border: OutlineInputBorder()),
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _addressController,
                        decoration: const InputDecoration(labelText: 'Address', border: OutlineInputBorder()),
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _phoneController,
                        decoration: const InputDecoration(labelText: 'Phone', border: OutlineInputBorder()),
                        keyboardType: TextInputType.phone,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _emailController,
                        decoration: const InputDecoration(labelText: 'Email', border: OutlineInputBorder()),
                        keyboardType: TextInputType.emailAddress,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _currencyController,
                        decoration: const InputDecoration(labelText: 'Currency Symbol', border: OutlineInputBorder()),
                        validator: (v) => (v == null || v.trim().isEmpty) ? 'Required' : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _reminderDaysController,
                        decoration: const InputDecoration(
                          labelText: 'Reminder days before expiry',
                          helperText: 'Flag a member as "expiring soon" this many days before their plan ends',
                          border: OutlineInputBorder(),
                        ),
                        keyboardType: TextInputType.number,
                        validator: (v) => (v == null || int.tryParse(v.trim()) == null) ? 'Enter a valid number' : null,
                      ),
                      const SizedBox(height: 8),
                      SwitchListTile(
                        title: const Text('SMS reminders enabled'),
                        subtitle: const Text('When off, reminders are only logged, never actually sent'),
                        value: _smsEnabled,
                        onChanged: (v) => setState(() => _smsEnabled = v),
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: 12),
                        Text(_error!, style: const TextStyle(color: Colors.red)),
                      ],
                      const SizedBox(height: 20),
                      FilledButton(
                        onPressed: _saving ? null : _save,
                        child: _saving
                            ? const SizedBox(
                                height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                            : const Text('Save'),
                      ),
                    ],
                  ),
                ),
    );
  }
}
