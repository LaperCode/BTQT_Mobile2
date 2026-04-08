import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/history_entry.dart';
import '../services/database_helper.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => HistoryScreenState();
}

class HistoryScreenState extends State<HistoryScreen> {
  List<HistoryEntry> _history = [];
  bool _isLoading = true;

  final _currencyFormat = NumberFormat.currency(
    locale: 'vi_VN',
    symbol: '₫',
    decimalDigits: 0,
  );

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    setState(() => _isLoading = true);
    final history = await DatabaseHelper.instance.getHistory();
    setState(() {
      _history = history;
      _isLoading = false;
    });
  }

  Future<void> loadHistory() => _loadHistory();

  Future<void> _deleteEntry(int id) async {
    await DatabaseHelper.instance.deleteHistory(id);
    _loadHistory();
  }

  Future<void> _clearAll() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E2E),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Xóa tất cả lịch sử?',
          style: TextStyle(color: Colors.white),
        ),
        content: const Text(
          'Hành động này không thể hoàn tác.',
          style: TextStyle(color: Colors.grey),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text('Xóa', style: TextStyle(color: Colors.red[400])),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await DatabaseHelper.instance.clearHistory();
      _loadHistory();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Header
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [Colors.amber[600]!, Colors.orange[700]!],
                  ),
                  borderRadius: BorderRadius.circular(14),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.amber.withValues(alpha: 0.3),
                      blurRadius: 8,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: const Icon(Icons.history, color: Colors.white, size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Lịch Sử Tra Cứu',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      '${_history.length} bản ghi',
                      style: TextStyle(fontSize: 13, color: Colors.grey[400]),
                    ),
                  ],
                ),
              ),
              if (_history.isNotEmpty)
                IconButton(
                  onPressed: _clearAll,
                  icon: Icon(Icons.delete_sweep, color: Colors.red[400]),
                  tooltip: 'Xóa tất cả',
                ),
            ],
          ),
        ),

        // Content
        Expanded(
          child: _isLoading
              ? const Center(
                  child: CircularProgressIndicator(color: Colors.amber),
                )
              : _history.isEmpty
              ? _buildEmptyState()
              : RefreshIndicator(
                  onRefresh: _loadHistory,
                  color: Colors.amber[700],
                  child: ListView.builder(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    itemCount: _history.length,
                    itemBuilder: (context, index) {
                      return _buildHistoryCard(_history[index]);
                    },
                  ),
                ),
        ),
      ],
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.history_toggle_off, size: 64, color: Colors.grey[700]),
          const SizedBox(height: 16),
          Text(
            'Chưa có lịch sử',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: Colors.grey[500],
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Hãy sử dụng Quy Đổi để tạo bản ghi đầu tiên',
            style: TextStyle(fontSize: 13, color: Colors.grey[600]),
          ),
        ],
      ),
    );
  }

  Widget _buildHistoryCard(HistoryEntry entry) {
    final dateStr = DateFormat('HH:mm - dd/MM/yyyy').format(entry.timestamp);

    return Dismissible(
      key: Key('history_${entry.id}'),
      direction: DismissDirection.endToStart,
      onDismissed: (_) => _deleteEntry(entry.id!),
      background: Container(
        margin: const EdgeInsets.only(bottom: 10),
        decoration: BoxDecoration(
          color: Colors.red[900],
          borderRadius: BorderRadius.circular(14),
        ),
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 20),
        child: const Icon(Icons.delete, color: Colors.white),
      ),
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        decoration: BoxDecoration(
          color: const Color(0xFF1E1E2E),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: Colors.amber.withValues(alpha: 0.1)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.amber.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      entry.goldType,
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: Colors.amber[300],
                      ),
                    ),
                  ),
                  const Spacer(),
                  Icon(Icons.access_time, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    dateStr,
                    style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '${entry.quantity} ${entry.unit}',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                  Text(
                    _currencyFormat.format(entry.totalValue),
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Colors.amber[400],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
