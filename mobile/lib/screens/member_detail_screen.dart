import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/member_view.dart';
import '../models/membership_plan.dart';
import '../services/api_client.dart';
import '../widgets/status_badge.dart';

class MemberDetailScreen extends StatefulWidget {
  final ApiClient apiClient;
  final int memberId;

  const MemberDetailScreen({super.key, required this.apiClient, required this.memberId});

  @override
  State<MemberDetailScreen> createState() => _MemberDetailScreenState();
}

class _MemberDetailScreenState extends State<MemberDetailScreen> {
  MemberDetail? _detail;
  bool _loading = true;
  String? _error;
  final _dateFormat = DateFormat('dd MMM yyyy');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final json = await widget.apiClient.get('/members/${widget.memberId}') as Map<String, dynamic>;
      setState(() => _detail = MemberDetail.fromJson(json));
    } catch (e) {
      setState(() => _error = 'Failed to load member: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _assignOrRenew() async {
    final detail = _detail;
    if (detail == null) return;
    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _AssignPlanSheet(apiClient: widget.apiClient, memberId: detail.member.memberId),
    );
    if (result == true) _load();
  }

  Future<void> _recordPayment() async {
    final detail = _detail;
    if (detail == null) return;
    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _RecordPaymentSheet(apiClient: widget.apiClient, memberId: detail.member.memberId),
    );
    if (result == true) _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_detail?.member.fullName ?? 'Member')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : _buildBody(_detail!),
      floatingActionButton: _detail == null
          ? null
          : Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                FloatingActionButton.extended(
                  heroTag: 'renew',
                  onPressed: _assignOrRenew,
                  icon: const Icon(Icons.card_membership),
                  label: const Text('Assign/Renew Plan'),
                ),
                const SizedBox(height: 12),
                FloatingActionButton.extended(
                  heroTag: 'payment',
                  onPressed: _recordPayment,
                  icon: const Icon(Icons.payments),
                  label: const Text('Record Payment'),
                ),
              ],
            ),
    );
  }

  Widget _buildBody(MemberDetail detail) {
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(detail.member.fullName, style: Theme.of(context).textTheme.titleLarge),
              StatusBadge.forMembership(detail.membershipStatus),
            ],
          ),
          const SizedBox(height: 4),
          if (detail.member.phone != null) Text(detail.member.phone!),
          if (detail.member.email != null) Text(detail.member.email!),
          const SizedBox(height: 16),
          if (detail.membershipStatus.currentSubscription != null) ...[
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Current Plan', style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 8),
                    Text(detail.membershipStatus.currentSubscription!.planName ?? 'Plan'),
                    Text(
                      '${_dateFormat.format(detail.membershipStatus.currentSubscription!.startDate)} - '
                      '${_dateFormat.format(detail.membershipStatus.currentSubscription!.endDate)}',
                    ),
                    Text(
                      detail.membershipStatus.daysRemaining >= 0
                          ? '${detail.membershipStatus.daysRemaining} day(s) remaining'
                          : '${-detail.membershipStatus.daysRemaining} day(s) overdue',
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
          ],
          Text('Subscription History', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          if (detail.subscriptionHistory.isEmpty) const Text('No subscriptions yet.'),
          ...detail.subscriptionHistory.map((s) => ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(s.planName ?? 'Plan #${s.planId}'),
                subtitle: Text('${_dateFormat.format(s.startDate)} - ${_dateFormat.format(s.endDate)}'),
                trailing: Text('Rs. ${s.amountCharged.toStringAsFixed(0)}'),
              )),
          const SizedBox(height: 16),
          Text('Payment History', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          if (detail.paymentHistory.isEmpty) const Text('No payments yet.'),
          ...detail.paymentHistory.map((p) => ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('Rs. ${p.amount.toStringAsFixed(0)} (${p.mode ?? 'N/A'})'),
                subtitle: Text(_dateFormat.format(p.paymentDate)),
              )),
          const SizedBox(height: 100),
        ],
      ),
    );
  }
}

class _AssignPlanSheet extends StatefulWidget {
  final ApiClient apiClient;
  final int memberId;

  const _AssignPlanSheet({required this.apiClient, required this.memberId});

  @override
  State<_AssignPlanSheet> createState() => _AssignPlanSheetState();
}

class _AssignPlanSheetState extends State<_AssignPlanSheet> {
  List<MembershipPlan> _plans = [];
  int? _selectedPlanId;
  String _mode = 'CASH';
  bool _loadingPlans = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadPlans();
  }

  Future<void> _loadPlans() async {
    try {
      final raw = await widget.apiClient.get('/plans') as List<dynamic>;
      final plans = raw.map((e) => MembershipPlan.fromJson(e as Map<String, dynamic>)).toList();
      setState(() {
        _plans = plans;
        _selectedPlanId = plans.isNotEmpty ? plans.first.planId : null;
      });
    } catch (e) {
      setState(() => _error = 'Failed to load plans: $e');
    } finally {
      setState(() => _loadingPlans = false);
    }
  }

  Future<void> _save() async {
    if (_selectedPlanId == null) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await widget.apiClient.post('/subscriptions', body: {
        'memberId': widget.memberId,
        'planId': _selectedPlanId,
        'mode': _mode,
      });
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      setState(() => _error = 'Failed to assign plan: $e');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 16,
        bottom: MediaQuery.of(context).viewInsets.bottom + 16,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Assign / Renew Plan', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 16),
          if (_loadingPlans)
            const Center(child: CircularProgressIndicator())
          else if (_plans.isEmpty)
            const Text('No active membership plans. Add one from the web dashboard first.')
          else ...[
            DropdownButtonFormField<int>(
              initialValue: _selectedPlanId,
              decoration: const InputDecoration(labelText: 'Plan', border: OutlineInputBorder()),
              items: _plans
                  .map((p) => DropdownMenuItem(
                      value: p.planId, child: Text('${p.name} - Rs. ${p.price.toStringAsFixed(0)}')))
                  .toList(),
              onChanged: (v) => setState(() => _selectedPlanId = v),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _mode,
              decoration: const InputDecoration(labelText: 'Payment Mode', border: OutlineInputBorder()),
              items: const ['CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE']
                  .map((m) => DropdownMenuItem(value: m, child: Text(m)))
                  .toList(),
              onChanged: (v) => setState(() => _mode = v ?? 'CASH'),
            ),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(_error!, style: const TextStyle(color: Colors.red)),
          ],
          const SizedBox(height: 20),
          FilledButton(
            onPressed: (_saving || _plans.isEmpty) ? null : _save,
            child: _saving
                ? const SizedBox(
                    height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Save'),
          ),
        ],
      ),
    );
  }
}

class _RecordPaymentSheet extends StatefulWidget {
  final ApiClient apiClient;
  final int memberId;

  const _RecordPaymentSheet({required this.apiClient, required this.memberId});

  @override
  State<_RecordPaymentSheet> createState() => _RecordPaymentSheetState();
}

class _RecordPaymentSheetState extends State<_RecordPaymentSheet> {
  final _formKey = GlobalKey<FormState>();
  final _amountController = TextEditingController();
  final _referenceController = TextEditingController();
  String _mode = 'CASH';
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _amountController.dispose();
    _referenceController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await widget.apiClient.post('/payments', body: {
        'memberId': widget.memberId,
        'amount': double.parse(_amountController.text.trim()),
        'mode': _mode,
        'referenceNo': _referenceController.text.trim(),
      });
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      setState(() => _error = 'Failed to record payment: $e');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 16,
        bottom: MediaQuery.of(context).viewInsets.bottom + 16,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Record Payment', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            TextFormField(
              controller: _amountController,
              decoration: const InputDecoration(labelText: 'Amount', prefixText: 'Rs. ', border: OutlineInputBorder()),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Required';
                return double.tryParse(v.trim()) == null ? 'Enter a valid number' : null;
              },
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _mode,
              decoration: const InputDecoration(labelText: 'Payment Mode', border: OutlineInputBorder()),
              items: const ['CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE']
                  .map((m) => DropdownMenuItem(value: m, child: Text(m)))
                  .toList(),
              onChanged: (v) => setState(() => _mode = v ?? 'CASH'),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _referenceController,
              decoration: const InputDecoration(labelText: 'Reference No. (optional)', border: OutlineInputBorder()),
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
