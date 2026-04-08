import 'package:flutter_test/flutter_test.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:btqt_bai1_flutter/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(() {
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
  });

  testWidgets('Gold Tracker app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const GoldTrackerApp());
    expect(find.text('Gold Tracker'), findsOneWidget);
  });
}
