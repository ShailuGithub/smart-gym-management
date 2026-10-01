import 'package:flutter/material.dart';

import '../models/member_view.dart';
import '../services/api_client.dart';
import '../widgets/status_badge.dart';
import 'member_detail_screen.dart';

class MembersScreen extends StatefulWidget {
  final ApiClient apiClient;
  final int branchId;

  const MembersScreen({super.key, required this.apiClient, required this.branchId});

  @override
  State<MembersScreen> createState() => _MembersScreenState();
}

class _MembersScreenState extends State<MembersScreen> {
  List<MemberView> _members = [];
  bool _loading = true;
  String? _error;
  String _query = '';

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
      final raw = await widget.apiClient
          .get('/members', query: {'branchId': widget.branchId.toString()}) as List<dynamic>;
      setState(() {
        _members = raw.map((e) => MemberView.fromJson(e as Map<String, dynamic>)).toList();
      });
    } catch (e) {
      setState(() => _error = 'Failed to load members: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _addMember() async {
    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _AddMemberSheet(apiClient: widget.apiClient, branchId: widget.branchId),
    );
    if (result == true) _load();
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _query.isEmpty
        ? _members
        : _members
            .where((m) => m.member.fullName.toLowerCase().contains(_query.toLowerCase()))
            .toList();

    return Scaffold(
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              decoration: const InputDecoration(
                prefixIcon: Icon(Icons.search),
                hintText: 'Search members by name',
                border: OutlineInputBorder(),
                isDense: true,
              ),
              onChanged: (v) => setState(() => _query = v),
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(child: Text(_error!))
                    : RefreshIndicator(
                        onRefresh: _load,
                        child: filtered.isEmpty
                            ? ListView(children: const [
                                Padding(
                                  padding: EdgeInsets.all(32),
                                  child: Center(child: Text('No members found.')),
                                )
                              ])
                            : ListView.separated(
                                itemCount: filtered.length,
                                separatorBuilder: (_, _) => const Divider(height: 1),
                                itemBuilder: (context, i) {
                                  final view = filtered[i];
                                  return ListTile(
                                    title: Text(view.member.fullName),
                                    subtitle: Text(view.member.phone ?? 'No phone on file'),
                                    trailing: StatusBadge.forMembership(view.membershipStatus),
                                    onTap: () async {
                                      await Navigator.of(context).push(MaterialPageRoute(
                                        builder: (_) => MemberDetailScreen(
                                          apiClient: widget.apiClient,
                                          memberId: view.member.memberId,
                                        ),
                                      ));
                                      _load();
                                    },
                                  );
                                },
                              ),
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _addMember,
        child: const Icon(Icons.person_add),
      ),
    );
  }
}

class _AddMemberSheet extends StatefulWidget {
  final ApiClient apiClient;
  final int branchId;

  const _AddMemberSheet({required this.apiClient, required this.branchId});

  @override
  State<_AddMemberSheet> createState() => _AddMemberSheetState();
}

class _AddMemberSheetState extends State<_AddMemberSheet> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await widget.apiClient.post('/members', body: {
        'fullName': _nameController.text.trim(),
        'phone': _phoneController.text.trim(),
        'email': _emailController.text.trim(),
        'branchId': widget.branchId,
      });
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      setState(() => _error = 'Failed to add member: $e');
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
            Text('Add Member', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: 'Full name', border: OutlineInputBorder()),
              validator: (v) => (v == null || v.trim().isEmpty) ? 'Required' : null,
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
