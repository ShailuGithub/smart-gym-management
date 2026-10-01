import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/api_user.dart';
import '../models/report_rows.dart';
import '../services/api_client.dart';
import '../widgets/status_badge.dart';

/// Three read-only report views, mirroring web/Forms/Reports/reports.jsp's tabs but without CSV
/// export (a web-only convenience for now) - Membership Due/Expiring, Revenue/Collection, and
/// (Admin-only, matching api.ReportApiServlet's rule) Branch-wise Active Members.
class ReportsScreen extends StatefulWidget {
  final ApiClient apiClient;
  final ApiUser user;
  final int branchId;

  const ReportsScreen({super.key, required this.apiClient, required this.user, required this.branchId});

  @override
  State<ReportsScreen> createState() => _ReportsScreenState();
}

class _ReportsScreenState extends State<ReportsScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: widget.user.isAdmin ? 3 : 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Reports'),
        bottom: TabBar(
          controller: _tabController,
          isScrollable: true,
          tabs: [
            const Tab(text: 'Due / Expiring'),
            const Tab(text: 'Revenue'),
            if (widget.user.isAdmin) const Tab(text: 'Branch Summary'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _DueTab(apiClient: widget.apiClient, branchId: widget.branchId),
          _RevenueTab(apiClient: widget.apiClient, branchId: widget.branchId),
          if (widget.user.isAdmin) _BranchSummaryTab(apiClient: widget.apiClient),
        ],
      ),
    );
  }
}

class _DueTab extends StatefulWidget {
  final ApiClient apiClient;
  final int branchId;

  const _DueTab({required this.apiClient, required this.branchId});

  @override
  State<_DueTab> createState() => _DueTabState();
}

class _DueTabState extends State<_DueTab> {
  List<DueRow> _rows = [];
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
      final raw = await widget.apiClient
          .get('/reports/due', query: {'branchId': widget.branchId.toString()}) as List<dynamic>;
      setState(() => _rows = raw.map((e) => DueRow.fromJson(e as Map<String, dynamic>)).toList());
    } catch (e) {
      setState(() => _error = 'Failed to load report: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) return Center(child: Text(_error!));
    if (_rows.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(children: const [
          Padding(padding: EdgeInsets.all(32), child: Center(child: Text('No members are due or expiring right now.')))
        ]),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        itemCount: _rows.length,
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, i) {
          final r = _rows[i];
          final daysText = r.status.daysRemaining >= 0
              ? '${r.status.daysRemaining} day(s) remaining'
              : '${-r.status.daysRemaining} day(s) overdue';
          return ListTile(
            title: Text(r.member.fullName),
            subtitle: Text('${r.branchName} - ${r.member.phone ?? 'No phone'}\n$daysText'),
            isThreeLine: true,
            leading: StatusBadge.forMembership(r.status),
          );
        },
      ),
    );
  }
}

class _RevenueTab extends StatefulWidget {
  final ApiClient apiClient;
  final int branchId;

  const _RevenueTab({required this.apiClient, required this.branchId});

  @override
  State<_RevenueTab> createState() => _RevenueTabState();
}

class _RevenueTabState extends State<_RevenueTab> {
  RevenueReport? _report;
  bool _loading = true;
  String? _error;
  late DateTime _start = DateTime(DateTime.now().year, DateTime.now().month, 1);
  DateTime _end = DateTime.now();
  final _dateFormat = DateFormat('dd MMM');

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
      final json = await widget.apiClient.get('/reports/revenue', query: {
        'branchId': widget.branchId.toString(),
        'start': DateFormat('yyyy-MM-dd').format(_start),
        'end': DateFormat('yyyy-MM-dd').format(_end),
      }) as Map<String, dynamic>;
      setState(() => _report = RevenueReport.fromJson(json));
    } catch (e) {
      setState(() => _error = 'Failed to load report: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _pickRange() async {
    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
      initialDateRange: DateTimeRange(start: _start, end: _end),
    );
    if (picked != null) {
      setState(() {
        _start = picked.start;
        _end = picked.end;
      });
      _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: OutlinedButton.icon(
            onPressed: _pickRange,
            icon: const Icon(Icons.date_range),
            label: Text('${_dateFormat.format(_start)} - ${_dateFormat.format(_end)}'),
          ),
        ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _error != null
                  ? Center(child: Text(_error!))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.separated(
                        itemCount: (_report?.rows.length ?? 0) + 1,
                        separatorBuilder: (_, _) => const Divider(height: 1),
                        itemBuilder: (context, i) {
                          final rows = _report!.rows;
                          if (i == rows.length) {
                            return ListTile(
                              title: const Text('Total', style: TextStyle(fontWeight: FontWeight.bold)),
                              trailing: Text(
                                'Rs. ${_report!.total.toStringAsFixed(0)}',
                                style: const TextStyle(fontWeight: FontWeight.bold),
                              ),
                            );
                          }
                          final r = rows[i];
                          return ListTile(
                            title: Text(DateFormat('dd MMM yyyy').format(r.date)),
                            trailing: Text(
                              'Rs. ${r.amount.toStringAsFixed(0)}',
                              style: TextStyle(color: r.amount > 0 ? Colors.green : null),
                            ),
                          );
                        },
                      ),
                    ),
        ),
      ],
    );
  }
}

class _BranchSummaryTab extends StatefulWidget {
  final ApiClient apiClient;

  const _BranchSummaryTab({required this.apiClient});

  @override
  State<_BranchSummaryTab> createState() => _BranchSummaryTabState();
}

class _BranchSummaryTabState extends State<_BranchSummaryTab> {
  List<BranchSummaryRow> _rows = [];
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
      final raw = await widget.apiClient.get('/reports/branch-summary') as List<dynamic>;
      setState(() => _rows = raw.map((e) => BranchSummaryRow.fromJson(e as Map<String, dynamic>)).toList());
    } catch (e) {
      setState(() => _error = 'Failed to load report: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) return Center(child: Text(_error!));
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        itemCount: _rows.length,
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, i) {
          final r = _rows[i];
          return ListTile(
            title: Text(r.branchName),
            subtitle: Text(
              'Active: ${r.active}  •  Expiring: ${r.expiringSoon}  •  Expired: ${r.expired}  •  No Plan: ${r.noPlan}',
            ),
          );
        },
      ),
    );
  }
}
