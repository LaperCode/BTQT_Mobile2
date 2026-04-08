import 'package:flutter/foundation.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/history_entry.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  // In-memory fallback for web platform
  static final List<HistoryEntry> _memoryHistory = [];
  static int _memoryId = 0;

  DatabaseHelper._init();

  bool get _isWeb => kIsWeb;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('gold_tracker.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);
    return await openDatabase(path, version: 1, onCreate: _createDB);
  }

  Future<void> _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        goldType TEXT NOT NULL,
        quantity REAL NOT NULL,
        unit TEXT NOT NULL,
        pricePerUnit REAL NOT NULL,
        totalValue REAL NOT NULL,
        timestamp TEXT NOT NULL
      )
    ''');
  }

  Future<int> insertHistory(HistoryEntry entry) async {
    if (_isWeb) {
      _memoryId++;
      _memoryHistory.insert(
        0,
        HistoryEntry(
          id: _memoryId,
          goldType: entry.goldType,
          quantity: entry.quantity,
          unit: entry.unit,
          pricePerUnit: entry.pricePerUnit,
          totalValue: entry.totalValue,
          timestamp: entry.timestamp,
        ),
      );
      return _memoryId;
    }
    final db = await database;
    return await db.insert('history', entry.toMap());
  }

  Future<List<HistoryEntry>> getHistory() async {
    if (_isWeb) {
      return List.from(_memoryHistory);
    }
    final db = await database;
    final maps = await db.query('history', orderBy: 'id DESC');
    return maps.map((map) => HistoryEntry.fromMap(map)).toList();
  }

  Future<int> deleteHistory(int id) async {
    if (_isWeb) {
      _memoryHistory.removeWhere((e) => e.id == id);
      return 1;
    }
    final db = await database;
    return await db.delete('history', where: 'id = ?', whereArgs: [id]);
  }

  Future<int> clearHistory() async {
    if (_isWeb) {
      _memoryHistory.clear();
      return 1;
    }
    final db = await database;
    return await db.delete('history');
  }
}
