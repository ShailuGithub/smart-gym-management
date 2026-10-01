import 'package:flutter/material.dart';

import '../models/due_member.dart';
import '../services/api_client.dart';
import '../widgets/status_badge.dart';

/// Members who are EXPIRING_SOON or EXPIRED for the branch (GET /api/reminders/due), with a
/// per-row "Send" action (POST /api/reminders/send) - mirrors servlet.ReminderServlet's web
/// screen but without the "Send to All" bulk action (kept to one-at-a-time on mobile for a
/// simpler first pass; the web dashboard already covers bulk sending).
class RemindersScreen extends StatefulWidget {
  final ApiClient apiClient;
  final int branchId;

  const RemindersScreen({super.key, required this.apiClient, required this.branchId});

  @override
  State<RemindersScreen> createState() => _RemindersScreenState();
}

class _RemindersScreenState extends State<RemindersScreen> {
  List<DueMember> _due = [];
  bool _loading = true;
  String? _error;
  final Set<int> _sending = {};

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
          .get('/reminders/due', query: {'branchId': widget.branchId.toString()}) as List<dynamic>;
      setState(() => _due = raw.map((e) => DueMember.fromJson(e as Map<String, dynamic>)).toList());
    } catch (e) {
      setState(() => _error = 'Failed to load reminders: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _send(DueMember dm) async {
    setState(() => _sending.add(dm.member.memberId));
    try {
      await widget.apiClient.post('/reminders/send', body: {'memberId': dm.member.memberId});
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Reminder sent to ${dm.member.fullName}')));
      }
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to send: $e')));
      }
    } finally {
      if (mounted) setState(() => _sending.remove(dm.member.memberId));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) return Center(child: Text(_error!));
    if (_due.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(children: const [
          Padding(
            padding: EdgeInsets.all(32),
            child: Center(child: Text('No members are due for a reminder right now.')),
          )
        ]),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        itemCount: _due.length,
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, i) {
          final dm = _due[i];
          final sending = _sending.contains(dm.member.memberId);
          final daysText = dm.status.daysRemaining >= 0
              ? '${dm.status.daysRemaining} day(s) remaining'
              : '${-dm.status.daysRemaining} day(s) overdue';
          return ListTile(
            title: Text(dm.member.fullName),
            subtitle: Text(
              '${dm.member.phone ?? 'No phone'} - $daysText'
              '${dm.lastReminder != null ? '\nLast reminder: ${dm.lastReminder!.status}' : ''}',
            ),
            isThreeLine: dm.lastReminder != null,
            leading: StatusBadge.forMembership(dm.status),
            trailing: sending
                ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : IconButton(
                    icon: const Icon(Icons.send),
                    tooltip: 'Send reminder',
                    onPressed: () => _send(dm),
                  ),
          );
        },
      ),
    );
  }
}
