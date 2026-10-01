import 'package:flutter/material.dart';

import '../models/api_user.dart';
import '../models/dashboard_stats.dart';
import '../models/gym_branch.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../widgets/stat_card.dart';
import 'login_screen.dart';
import 'members_screen.dart';
import 'payments_screen.dart';
import 'plans_screen.dart';
import 'reminders_screen.dart';
import 'reports_screen.dart';
import 'settings_screen.dart';

/// The app's home shell after login: an app bar with a branch switcher (ADMIN only, and only
/// when more than one branch exists - a MANAGER/STAFF is always locked to their own, mirroring
/// BranchContext's rule on the web) plus a bottom nav bar across Dashboard/Members/Payments/
/// Reminders, each a separate tab body sharing the same selected branch.
class DashboardScreen extends StatefulWidget {
  final AuthService authService;
  final ApiClient apiClient;

  const DashboardScreen({super.key, required this.authService, required this.apiClient});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  int _tabIndex = 0;
  ApiUser? _user;
  List<GymBranch> _branches = [];
  int? _selectedBranchId;
  bool _loadingShell = true;
  String? _shellError;

  @override
  void initState() {
    super.initState();
    _loadShell();
  }

  Future<void> _loadShell() async {
    setState(() {
      _loadingShell = true;
      _shellError = null;
    });
    try {
      final user = await widget.authService.currentUser();
      List<GymBranch> branches = [];
      if (user != null) {
        final raw = await widget.apiClient.get('/branches') as List<dynamic>;
        branches = raw.map((e) => GymBranch.fromJson(e as Map<String, dynamic>)).toList();
      }
      setState(() {
        _user = user;
        _branches = branches;
        _selectedBranchId = user != null && !user.isAdmin
            ? user.branchId
            : (branches.isNotEmpty ? branches.first.branchId : null);
      });
    } catch (e) {
      setState(() => _shellError = 'Could not load branches: $e');
    } finally {
      setState(() => _loadingShell = false);
    }
  }

  Future<void> _logout() async {
    await widget.authService.logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(
        builder: (_) => LoginScreen(authService: widget.authService, apiClient: widget.apiClient),
      ),
      (route) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loadingShell) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_shellError != null) {
      return Scaffold(
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(_shellError!, textAlign: TextAlign.center),
                const SizedBox(height: 16),
                FilledButton(onPressed: _loadShell, child: const Text('Retry')),
              ],
            ),
          ),
        ),
      );
    }
    if (_branches.isEmpty) {
      return Scaffold(
        appBar: AppBar(title: const Text('Smart Gym Management'), actions: [
          IconButton(onPressed: _logout, icon: const Icon(Icons.logout)),
        ]),
        body: const Center(
          child: Padding(
            padding: EdgeInsets.all(24),
            child: Text(
              'No branches yet. Ask an admin to add a branch from the web dashboard first.',
              textAlign: TextAlign.center,
            ),
          ),
        ),
      );
    }

    final branchId = _selectedBranchId!;
    final tabs = [
      _DashboardTab(apiClient: widget.apiClient, branchId: branchId, key: ValueKey('dash-$branchId')),
      MembersScreen(apiClient: widget.apiClient, branchId: branchId, key: ValueKey('members-$branchId')),
      PaymentsScreen(apiClient: widget.apiClient, branchId: branchId, key: ValueKey('payments-$branchId')),
      RemindersScreen(apiClient: widget.apiClient, branchId: branchId, key: ValueKey('reminders-$branchId')),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Smart Gym Management'),
        actions: [
          if (_user != null && _user!.isAdmin && _branches.length > 1)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<int>(
                  value: branchId,
                  dropdownColor: Theme.of(context).colorScheme.surface,
                  style: TextStyle(color: Theme.of(context).appBarTheme.foregroundColor),
                  items: _branches
                      .map((b) => DropdownMenuItem(value: b.branchId, child: Text(b.name)))
                      .toList(),
                  onChanged: (v) => setState(() => _selectedBranchId = v),
                ),
              ),
            ),
          IconButton(onPressed: _logout, icon: const Icon(Icons.logout)),
        ],
      ),
      drawer: _buildDrawer(branchId),
      body: IndexedStack(index: _tabIndex, children: tabs),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tabIndex,
        onDestinationSelected: (i) => setState(() => _tabIndex = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Dashboard'),
          NavigationDestination(icon: Icon(Icons.people_outline), selectedIcon: Icon(Icons.people), label: 'Members'),
          NavigationDestination(icon: Icon(Icons.payments_outlined), selectedIcon: Icon(Icons.payments), label: 'Payments'),
          NavigationDestination(icon: Icon(Icons.notifications_outlined), selectedIcon: Icon(Icons.notifications), label: 'Reminders'),
        ],
      ),
    );
  }

  /// Occasional/admin screens live behind a drawer rather than crowding the bottom nav bar,
  /// mirroring the web sidebar's "Main" vs "Admin" split (see web/header.jsp): Membership Plans
  /// and Reports need Admin/Manager (canAccessPlans()/canAccessReports()); Settings is Admin-only,
  /// matching header.jsp's `isAdmin` guard on that nav item.
  Widget _buildDrawer(int branchId) {
    final user = _user!;
    return Drawer(
      child: SafeArea(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            DrawerHeader(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Text(user.fullName, style: Theme.of(context).textTheme.titleLarge),
                  Text(user.role, style: Theme.of(context).textTheme.bodyMedium),
                ],
              ),
            ),
            if (user.canAccessPlans)
              ListTile(
                leading: const Icon(Icons.card_membership_outlined),
                title: const Text('Membership Plans'),
                onTap: () {
                  Navigator.of(context).pop();
                  Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => PlansScreen(apiClient: widget.apiClient, user: user),
                  ));
                },
              ),
            if (user.canAccessReports)
              ListTile(
                leading: const Icon(Icons.bar_chart_outlined),
                title: const Text('Reports'),
                onTap: () {
                  Navigator.of(context).pop();
                  Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => ReportsScreen(apiClient: widget.apiClient, user: user, branchId: branchId),
                  ));
                },
              ),
            if (user.isAdmin)
              ListTile(
                leading: const Icon(Icons.settings_outlined),
                title: const Text('Settings'),
                onTap: () {
                  Navigator.of(context).pop();
                  Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => SettingsScreen(apiClient: widget.apiClient),
                  ));
                },
              ),
          ],
        ),
      ),
    );
  }
}

class _DashboardTab extends StatefulWidget {
  final ApiClient apiClient;
  final int branchId;

  const _DashboardTab({super.key, required this.apiClient, required this.branchId});

  @override
  State<_DashboardTab> createState() => _DashboardTabState();
}

class _DashboardTabState extends State<_DashboardTab> {
  Future<DashboardStats>? _future;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<DashboardStats> _load() async {
    final json = await widget.apiClient
        .get('/dashboard', query: {'branchId': widget.branchId.toString()}) as Map<String, dynamic>;
    return DashboardStats.fromJson(json);
  }

  Future<void> _refresh() async {
    final future = _load();
    setState(() => _future = future);
    await future;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<DashboardStats>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('Failed to load dashboard: ${snapshot.error}'));
        }
        final stats = snapshot.data!;
        return RefreshIndicator(
          onRefresh: _refresh,
          child: GridView.count(
            padding: const EdgeInsets.all(16),
            crossAxisCount: 2,
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: 1.6,
            children: [
              StatCard(
                label: 'Active Members',
                value: stats.activeMembers.toString(),
                icon: Icons.people,
                color: Colors.blue,
              ),
              StatCard(
                label: 'Expiring Soon',
                value: stats.expiringSoon.toString(),
                icon: Icons.timelapse,
                color: Colors.orange,
              ),
              StatCard(
                label: 'Expired / No Plan',
                value: stats.expiredOrNoPlan.toString(),
                icon: Icons.error_outline,
                color: Colors.red,
              ),
              StatCard(
                label: "Today's Collection",
                value: 'Rs. ${stats.todaysCollection.toStringAsFixed(0)}',
                icon: Icons.currency_rupee,
                color: Colors.green,
              ),
            ],
          ),
        );
      },
    );
  }
}
