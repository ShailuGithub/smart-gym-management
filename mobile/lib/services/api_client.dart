import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;

import 'auth_service.dart';

/// Thrown for any non-2xx API response. `message` is parsed from the API's consistent
/// {"error": "..."} shape (see api.JsonUtil.writeError on the server).
class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

/// Thin wrapper around package:http for the Smart Gym Management JSON API (src/api/*.java).
/// Attaches the bearer token from AuthService to every request except login. Change [baseUrl] to
/// point at your deployed GlassFish server before running the app.
class ApiClient {
  /// Defaults per platform, both pointing at a GlassFish server on your own development PC:
  ///   - Web (kIsWeb): the browser shares the host's network stack directly, so `localhost` works.
  ///   - Android emulator: `10.0.2.2` is the emulator's alias for the host machine's `localhost`.
  /// Neither default works for:
  ///   - iOS simulator: use `http://localhost:8080/...` instead.
  ///   - A physical phone/tablet (even over the same Wi-Fi): use your PC's actual LAN IP, e.g.
  ///     `http://YOUR-SERVER-IP:8080/SmartGymManagement/api` (find it with `ipconfig`).
  ///   - A server that isn't on your dev machine at all: use its real hostname/IP.
  /// Change this one field - nothing else in the app hardcodes a host.
  static String baseUrl = kIsWeb
      ? 'http://localhost:8080/SmartGymManagement/api'
      : 'http://10.0.2.2:8080/SmartGymManagement/api';

  final AuthService authService;

  ApiClient(this.authService);

  static Uri buildUri(String path, [Map<String, String>? query]) {
    final uri = Uri.parse('$baseUrl$path');
    return query == null || query.isEmpty ? uri : uri.replace(queryParameters: query);
  }

  /// Decodes a response body as JSON, throwing [ApiException] for any non-2xx status using the
  /// API's consistent {"error": "..."} shape. Shared with AuthService's own raw login/logout
  /// calls (which can't go through the instance methods below - they run before/without a token).
  static dynamic decode(http.Response response) {
    final body = response.body.isEmpty ? null : jsonDecode(response.body);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = (body is Map && body['error'] is String)
          ? body['error'] as String
          : 'Request failed (HTTP ${response.statusCode}).';
      throw ApiException(response.statusCode, message);
    }
    return body;
  }

  Future<Map<String, String>> _headers() async {
    final headers = <String, String>{'Content-Type': 'application/json'};
    final token = await authService.token();
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  Future<dynamic> get(String path, {Map<String, String>? query}) async {
    final response = await http.get(buildUri(path, query), headers: await _headers());
    return decode(response);
  }

  Future<dynamic> post(String path, {Object? body}) async {
    final response = await http.post(
      buildUri(path),
      headers: await _headers(),
      body: body == null ? null : jsonEncode(body),
    );
    return decode(response);
  }

  Future<dynamic> put(String path, {Object? body}) async {
    final response = await http.put(
      buildUri(path),
      headers: await _headers(),
      body: body == null ? null : jsonEncode(body),
    );
    return decode(response);
  }

  Future<dynamic> delete(String path) async {
    final response = await http.delete(buildUri(path), headers: await _headers());
    return decode(response);
  }
}
