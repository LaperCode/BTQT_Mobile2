import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'screens/dashboard_screen.dart';
import 'screens/converter_screen.dart';
import 'screens/history_screen.dart';
import 'screens/chart_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env');
  // Initialize sqflite FFI for desktop platforms (Windows/Linux/macOS)
  if (!kIsWeb) {
    final platform = defaultTargetPlatform;
    if (platform == TargetPlatform.windows ||
        platform == TargetPlatform.linux ||
        platform == TargetPlatform.macOS) {
      sqfliteFfiInit();
      databaseFactory = databaseFactoryFfi;
    }
  }
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
    ),
  );
  runApp(const GoldTrackerApp());
}

class GoldTrackerApp extends StatelessWidget {
  const GoldTrackerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Gold Tracker',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF121220),
        primarySwatch: Colors.amber,
        colorScheme: ColorScheme.dark(
          primary: Colors.amber[600]!,
          secondary: Colors.amber[400]!,
          surface: const Color(0xFF1E1E2E),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF121220),
          elevation: 0,
          centerTitle: true,
        ),
        fontFamily: 'Roboto',
      ),
      home: const MainScreen(),
    );
  }
}

// MainScreen manages the bottom navigation and displays the corresponding screen for each tab
class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;
  final _historyKey = GlobalKey<HistoryScreenState>();

  late final List<Widget> _screens = [
    const DashboardScreen(),
    const ConverterScreen(),
    HistoryScreen(key: _historyKey),
    const ChartScreen(),
  ];

  final List<String> _titles = [
    'Gold Tracker',
    'Quy Đổi',
    'Lịch Sử',
    'Biểu Đồ',
  ];

  void _onTabTapped(int index) {
    setState(() => _currentIndex = index);
    // Refresh history data when switching to History tab
    if (index == 2) {
      _historyKey.currentState?.loadHistory();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.monetization_on, color: Colors.amber[400], size: 26),
            const SizedBox(width: 8),
            Text(
              _titles[_currentIndex],
              style: const TextStyle(
                fontWeight: FontWeight.w700,
                fontSize: 20,
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
      body: IndexedStack(index: _currentIndex, children: _screens),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: const Color(0xFF1A1A2E),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.3),
              blurRadius: 10,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: BottomNavigationBar(
          currentIndex: _currentIndex,
          onTap: _onTabTapped,
          type: BottomNavigationBarType.fixed,
          backgroundColor: Colors.transparent,
          selectedItemColor: Colors.amber[400],
          unselectedItemColor: Colors.grey[600],
          selectedFontSize: 12,
          unselectedFontSize: 11,
          elevation: 0,
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.dashboard),
              activeIcon: Icon(Icons.dashboard_rounded),
              label: 'Bảng giá',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.currency_exchange),
              activeIcon: Icon(Icons.currency_exchange),
              label: 'Quy đổi',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.history),
              activeIcon: Icon(Icons.history_rounded),
              label: 'Lịch sử',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.show_chart),
              activeIcon: Icon(Icons.show_chart_rounded),
              label: 'Biểu đồ',
            ),
          ],
        ),
      ),
    );
  }
}
