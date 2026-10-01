import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:smart_gym_mobile/main.dart';

void main() {
  testWidgets('App starts on the login screen when logged out', (WidgetTester tester) async {
    // No platform channel in a widget test - seed the in-memory fake so
    // AuthService.isLoggedIn() resolves instead of throwing.
    SharedPreferences.setMockInitialValues({});
    await tester.pumpWidget(const SmartGymApp());
    // Let the pending isLoggedIn() future (backed by the mocked shared_preferences) resolve.
    await tester.pumpAndSettle();
    expect(find.text('Smart Gym Management'), findsWidgets);
    expect(find.widgetWithText(TextFormField, 'Username'), findsOneWidget);
  });
}
