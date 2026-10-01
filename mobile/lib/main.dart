import 'package:flutter/material.dart';

import 'screens/dashboard_screen.dart';
import 'screens/login_screen.dart';
import 'services/api_client.dart';
import 'services/auth_service.dart';

void main() {
  runApp(const SmartGymApp());
}

class SmartGymApp extends StatelessWidget {
  const SmartGymApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Smart Gym Management',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorSchemeSeed: Colors.indigo,
        useMaterial3: true,
      ),
      home: const _AppRoot(),
    );
  }
}

/// Decides between LoginScreen and DashboardScreen based on whether a bearer token is already
/// persisted (see AuthService) - so a returning user skips the login form on app restart.
class _AppRoot extends StatefulWidget {
  const _AppRoot();

  @override
  State<_AppRoot> createState() => _AppRootState();
}

class _AppRootState extends State<_AppRoot> {
  late final AuthService _authService;
  late final ApiClient _apiClient;
  bool _checking = true;
  bool _loggedIn = false;

  @override
  void initState() {
    super.initState();
    _authService = AuthService();
    _apiClient = ApiClient(_authService);
    _authService.isLoggedIn().then((loggedIn) {
      setState(() {
        _loggedIn = loggedIn;
        _checking = false;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_checking) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return _loggedIn
        ? DashboardScreen(authService: _authService, apiClient: _apiClient)
        : LoginScreen(authService: _authService, apiClient: _apiClient);
  }
}
