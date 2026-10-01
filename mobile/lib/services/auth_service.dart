import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../models/api_user.dart';
import 'api_client.dart';

/// Handles login/logout and persists the bearer token + logged-in user across app restarts via
/// shared_preferences. Deliberately does NOT depend on ApiClient (that class depends on this one
/// for its Authorization header) - login/logout make their own raw http calls, sharing only
/// ApiClient's static uri-building/error-decoding helpers.
class AuthService {
  static const _tokenKey = 'auth_token';
  static const _userKey = 'auth_user';

  ApiUser? _cachedUser;

  Future<String?> token() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  Future<bool> isLoggedIn() async => (await token()) != null;

  /// The logged-in user, cached in memory after the first read this app session.
  Future<ApiUser?> currentUser() async {
    if (_cachedUser != null) return _cachedUser;
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_userKey);
    if (raw == null) return null;
    _cachedUser = ApiUser.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    return _cachedUser;
  }

  /// POST /auth/login. Throws [ApiException] on invalid credentials or a locked account (see
  /// api.AuthApiServlet on the server for the exact messages).
  Future<ApiUser> login(String username, String password) async {
    final response = await http.post(
      ApiClient.buildUri('/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'username': username, 'password': password}),
    );
    final json = ApiClient.decode(response) as Map<String, dynamic>;
    final token = json['token'] as String;
    final user = ApiUser.fromJson(json['user'] as Map<String, dynamic>);

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
    await prefs.setString(_userKey, jsonEncode(user.toJson()));
    _cachedUser = user;
    return user;
  }

  /// POST /auth/logout (best-effort - the token is cleared locally regardless of whether the
  /// server call succeeds, e.g. if the device is offline) then clears local storage.
  Future<void> logout() async {
    final currentToken = await token();
    if (currentToken != null) {
      try {
        await http.post(
          ApiClient.buildUri('/auth/logout'),
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer $currentToken',
          },
        );
      } catch (_) {
        // Offline or server error - still proceed to clear local state below.
      }
    }
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userKey);
    _cachedUser = null;
  }
}
