import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';
import '../services/gold_api_service.dart';

class ChartScreen extends StatefulWidget {
  const ChartScreen({super.key});

  @override
  State<ChartScreen> createState() => _ChartScreenState();
}

class _ChartScreenState extends State<ChartScreen> {
  final GoldApiService _apiService = GoldApiService();
  List<Map<String, dynamic>> _history = [];
  bool _isLoading = true;
  String _selectedType = 'Vàng SJC';
  List<String> _availableTypes = [];

  final _currencyFormat = NumberFormat.currency(
    locale: 'vi_VN',
    symbol: '₫',
    decimalDigits: 0,
  );

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final history = await _apiService.fetch7DayHistory();
      if (history.isNotEmpty) {
        final firstPrices = history.first['prices'] as Map<String, double>;
        _availableTypes = firstPrices.keys.toList();
      }
      setState(() {
        _history = history;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: Colors.amber),
      );
    }

    if (_history.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.bar_chart, size: 64, color: Colors.grey[700]),
            const SizedBox(height: 16),
            Text('Không có dữ liệu biểu đồ',
                style: TextStyle(color: Colors.grey[500], fontSize: 16)),
          ],
        ),
      );
    }

    // Compute data points for selected type
    final spots = <FlSpot>[];
    final labels = <String>[];
    double minY = double.infinity;
    double maxY = 0;

    for (int i = 0; i < _history.length; i++) {
      final prices = _history[i]['prices'] as Map<String, double>;
      final date = _history[i]['date'] as DateTime;
      final price = prices[_selectedType] ?? 0;
      spots.add(FlSpot(i.toDouble(), price / 1000000)); // In millions
      labels.add(DateFormat('dd/MM').format(date));
      if (price / 1000000 < minY) minY = price / 1000000;
      if (price / 1000000 > maxY) maxY = price / 1000000;
    }

    final padding = (maxY - minY) * 0.15;
    if (padding == 0) {
      minY = minY * 0.99;
      maxY = maxY * 1.01;
    } else {
      minY -= padding;
      maxY += padding;
    }

    final latestPrice =
        (_history.last['prices'] as Map<String, double>)[_selectedType] ?? 0;
    final firstPrice =
        (_history.first['prices'] as Map<String, double>)[_selectedType] ?? 0;
    final change = latestPrice - firstPrice;
    final changePercent = firstPrice > 0 ? (change / firstPrice * 100) : 0.0;
    final isUp = change >= 0;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
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
                child: const Icon(Icons.trending_up,
                    color: Colors.white, size: 24),
              ),
              const SizedBox(width: 14),
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Biểu Đồ Biến Động',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  SizedBox(height: 2),
                  Text(
                    '7 ngày gần nhất',
                    style: TextStyle(fontSize: 13, color: Colors.grey),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 20),

          // Type selector
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            decoration: BoxDecoration(
              color: const Color(0xFF1E1E2E),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: Colors.grey[700]!),
            ),
            child: DropdownButton<String>(
              value: _selectedType,
              isExpanded: true,
              underline: const SizedBox(),
              dropdownColor: const Color(0xFF1E1E2E),
              icon: Icon(Icons.keyboard_arrow_down, color: Colors.amber[400]),
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
              items: _availableTypes.map((type) {
                return DropdownMenuItem(
                  value: type,
                  child: Text(type),
                );
              }).toList(),
              onChanged: (v) {
                if (v != null) setState(() => _selectedType = v);
              },
            ),
          ),
          const SizedBox(height: 20),

          // Summary card
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: const Color(0xFF1E1E2E),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: isUp
                    ? Colors.green.withValues(alpha: 0.2)
                    : Colors.red.withValues(alpha: 0.2),
              ),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Giá hiện tại',
                        style: TextStyle(
                          fontSize: 13,
                          color: Colors.grey[400],
                        ),
                      ),
                      const SizedBox(height: 4),
                      FittedBox(
                        child: Text(
                          _currencyFormat.format(latestPrice),
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: isUp
                        ? Colors.green.withValues(alpha: 0.15)
                        : Colors.red.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        isUp ? Icons.trending_up : Icons.trending_down,
                        size: 18,
                        color: isUp ? Colors.greenAccent : Colors.redAccent,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${isUp ? "+" : ""}${changePercent.toStringAsFixed(2)}%',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: isUp ? Colors.greenAccent : Colors.redAccent,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // Chart
          Container(
            width: double.infinity,
            height: 280,
            padding: const EdgeInsets.fromLTRB(8, 24, 20, 12),
            decoration: BoxDecoration(
              color: const Color(0xFF1E1E2E),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: Colors.amber.withValues(alpha: 0.1),
              ),
            ),
            child: LineChart(
              LineChartData(
                minY: minY,
                maxY: maxY,
                gridData: FlGridData(
                  show: true,
                  drawVerticalLine: false,
                  horizontalInterval: (maxY - minY) / 4,
                  getDrawingHorizontalLine: (value) {
                    return FlLine(
                      color: Colors.grey[800]!,
                      strokeWidth: 0.5,
                    );
                  },
                ),
                titlesData: FlTitlesData(
                  leftTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 50,
                      getTitlesWidget: (value, meta) {
                        return Text(
                          '${value.toStringAsFixed(1)}tr',
                          style: TextStyle(
                            fontSize: 10,
                            color: Colors.grey[500],
                          ),
                        );
                      },
                    ),
                  ),
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      getTitlesWidget: (value, meta) {
                        final idx = value.toInt();
                        if (idx >= 0 && idx < labels.length) {
                          return Padding(
                            padding: const EdgeInsets.only(top: 8),
                            child: Text(
                              labels[idx],
                              style: TextStyle(
                                fontSize: 10,
                                color: Colors.grey[500],
                              ),
                            ),
                          );
                        }
                        return const Text('');
                      },
                    ),
                  ),
                  rightTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  topTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                ),
                borderData: FlBorderData(show: false),
                lineBarsData: [
                  LineChartBarData(
                    spots: spots,
                    isCurved: true,
                    curveSmoothness: 0.3,
                    color: Colors.amber[400],
                    barWidth: 3,
                    dotData: FlDotData(
                      show: true,
                      getDotPainter: (spot, percent, barData, index) {
                        return FlDotCirclePainter(
                          radius: 4,
                          color: Colors.amber[400]!,
                          strokeWidth: 2,
                          strokeColor: Colors.white,
                        );
                      },
                    ),
                    belowBarData: BarAreaData(
                      show: true,
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.amber.withValues(alpha: 0.3),
                          Colors.amber.withValues(alpha: 0.0),
                        ],
                      ),
                    ),
                  ),
                ],
                lineTouchData: LineTouchData(
                  touchTooltipData: LineTouchTooltipData(
                    getTooltipColor: (touchedSpot) =>
                        const Color(0xFF2A2A3E),
                    getTooltipItems: (touchedSpots) {
                      return touchedSpots.map((spot) {
                        return LineTooltipItem(
                          '${_currencyFormat.format(spot.y * 1000000)}\n',
                          const TextStyle(
                            color: Colors.amber,
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                          children: [
                            TextSpan(
                              text: labels[spot.x.toInt()],
                              style: TextStyle(
                                color: Colors.grey[400],
                                fontSize: 12,
                                fontWeight: FontWeight.normal,
                              ),
                            ),
                          ],
                        );
                      }).toList();
                    },
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Legend
          Center(
            child: Text(
              'Đơn vị: triệu VNĐ/Lượng • Chạm biểu đồ để xem chi tiết',
              style: TextStyle(fontSize: 11, color: Colors.grey[600]),
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
