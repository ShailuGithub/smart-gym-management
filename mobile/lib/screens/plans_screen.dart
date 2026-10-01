import 'package:flutter/material.dart';

import '../models/api_user.dart';
import '../models/membership_plan.dart';
import '../services/api_client.dart';

/// Membership Plans - business-wide (no branch scope), mirroring servlet.PlanServlet/
/// web/Forms/Masters/plans.jsp: Admin+Manager can view/add/edit (canAccessPlans() gates entry to
/// this screen from the drawer already), only Admin can delete a plan.
class PlansScreen extends StatefulWidget {
  final ApiClient apiClient;
  final ApiUser user;

  const PlansScreen({super.key, required this.apiClient, required this.user});

  @override
  State<PlansScreen> createState() => _PlansScreenState();
}

class _PlansScreenState extends State<PlansScreen> {
  List<MembershipPlan> _plans = [];
  bool _loading = true;
  String? _error;

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
      // ?all=true also returns INACTIVE plans (api.PlanApiServlet), matching what the web Plans
      // screen shows - the plain GET /api/plans (no query) is active-only, used by the assign/
      // renew picker on the member detail screen.
      final raw = await widget.apiClient.get('/plans', query: {'all': 'true'}) as List<dynamic>;
      setState(() => _plans = raw.map((e) => MembershipPlan.fromJson(e as Map<String, dynamic>)).toList());
    } catch (e) {
      setState(() => _error = 'Failed to load plans: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _addOrEdit([MembershipPlan? existing]) async {
    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _PlanFormSheet(apiClient: widget.apiClient, existing: existing),
    );
    if (result == true) _load();
  }

  Future<void> _delete(MembershipPlan plan) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Delete Plan'),
        content: Text('Delete "${plan.name}"? Members already on this plan keep their history.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.of(context).pop(true), child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await widget.apiClient.delete('/plans/${plan.planId}');
      _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to delete: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Membership Plans')),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _addOrEdit(),
        child: const Icon(Icons.add),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : _plans.isEmpty
                  ? RefreshIndicator(
                      onRefresh: _load,
                      child: ListView(children: const [
                        Padding(padding: EdgeInsets.all(32), child: Center(child: Text('No plans yet - add one above.')))
                      ]),
                    )
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.separated(
                        itemCount: _plans.length,
                        separatorBuilder: (_, _) => const Divider(height: 1),
                        itemBuilder: (context, i) {
                          final p = _plans[i];
                          return ListTile(
                            onTap: () => _addOrEdit(p),
                            title: Text(p.name),
                            subtitle: Text(
                              '${p.durationDays} days${p.description != null && p.description!.isNotEmpty ? ' - ${p.description}' : ''}'
                              '${p.status == 'INACTIVE' ? ' (Inactive)' : ''}',
                            ),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text('Rs. ${p.price.toStringAsFixed(0)}', style: const TextStyle(fontWeight: FontWeight.bold)),
                                if (widget.user.isAdmin)
                                  IconButton(
                                    icon: const Icon(Icons.delete_outline),
                                    tooltip: 'Delete',
                                    onPressed: () => _delete(p),
                                  ),
                              ],
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}

class _PlanFormSheet extends StatefulWidget {
  final ApiClient apiClient;
  final MembershipPlan? existing;

  const _PlanFormSheet({required this.apiClient, this.existing});

  @override
  State<_PlanFormSheet> createState() => _PlanFormSheetState();
}

class _PlanFormSheetState extends State<_PlanFormSheet> {
  final _formKey = GlobalKey<FormState>();
  late final _nameController =
      TextEditingController(text: widget.existing?.name ?? '');
  late final _durationController =
      TextEditingController(text: widget.existing?.durationDays.toString() ?? '30');
  late final _priceController =
      TextEditingController(text: widget.existing?.price.toStringAsFixed(0) ?? '');
  late final _descriptionController =
      TextEditingController(text: widget.existing?.description ?? '');
  late String _status = widget.existing?.status ?? 'ACTIVE';
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _nameController.dispose();
    _durationController.dispose();
    _priceController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    final body = {
      'name': _nameController.text.trim(),
      'durationDays': int.parse(_durationController.text.trim()),
      'price': double.parse(_priceController.text.trim()),
      'description': _descriptionController.text.trim(),
      'status': _status,
    };
    try {
      if (widget.existing == null) {
        await widget.apiClient.post('/plans', body: body);
      } else {
        await widget.apiClient.put('/plans/${widget.existing!.planId}', body: body);
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      setState(() => _error = 'Failed to save: $e');
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
            Text(widget.existing == null ? 'Add Plan' : 'Edit Plan', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: 'Plan Name', border: OutlineInputBorder()),
              validator: (v) => (v == null || v.trim().isEmpty) ? 'Required' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _durationController,
              decoration: const InputDecoration(labelText: 'Duration (days)', border: OutlineInputBorder()),
              keyboardType: TextInputType.number,
              validator: (v) => (v == null || int.tryParse(v.trim()) == null) ? 'Enter a valid number' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _priceController,
              decoration: const InputDecoration(labelText: 'Price', prefixText: 'Rs. ', border: OutlineInputBorder()),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: (v) => (v == null || double.tryParse(v.trim()) == null) ? 'Enter a valid number' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _descriptionController,
              decoration: const InputDecoration(labelText: 'Description (optional)', border: OutlineInputBorder()),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _status,
              decoration: const InputDecoration(labelText: 'Status', border: OutlineInputBorder()),
              items: const [
                DropdownMenuItem(value: 'ACTIVE', child: Text('Active')),
                DropdownMenuItem(value: 'INACTIVE', child: Text('Inactive')),
              ],
              onChanged: (v) => setState(() => _status = v ?? 'ACTIVE'),
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
