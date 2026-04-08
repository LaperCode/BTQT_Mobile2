import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/gold_price.dart';
import '../services/gold_api_service.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen>
    with SingleTickerProviderStateMixin {
  final GoldApiService _apiService = GoldApiService();
  List<GoldPrice> _prices = [];
  bool _isLoading = true;
  String? _error;
  late AnimationController _animController;

  final _currencyFormat = NumberFormat.currency(
    locale: 'vi_VN',
    symbol: '₫',
    decimalDigits: 0,
  );

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _loadPrices();
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  Future<void> _loadPrices() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final prices = await _apiService.fetchGoldPrices();
      setState(() {
        _prices = prices;
        _isLoading = false;
      });
      _animController.forward(from: 0);
    } catch (e) {
      setState(() {
        _error = 'Không thể tải giá vàng. Vui lòng thử lại.';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _loadPrices,
      color: Colors.amber[700],
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(child: _buildHeader()),
          if (_isLoading)
            const SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(color: Colors.amber),
              ),
            )
          else if (_error != null)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.error_outline, size: 48, color: Colors.red[300]),
                    const SizedBox(height: 12),
                    Text(_error!, style: TextStyle(color: Colors.red[300])),
                    const SizedBox(height: 12),
                    ElevatedButton(
                      onPressed: _loadPrices,
                      child: const Text('Thử lại'),
                    ),
                  ],
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    return AnimatedBuilder(
                      animation: _animController,
                      builder: (context, child) {
                        final delay = index * 0.15;
                        final animValue = (_animController.value - delay)
                            .clamp(0.0, 1.0);
                        return Transform.translate(
                          offset: Offset(0, 30 * (1 - animValue)),
                          child: Opacity(
                            opacity: animValue,
                            child: child,
                          ),
                        );
                      },
                      child: _buildPriceCard(_prices[index]),
                    );
                  },
                  childCount: _prices.length,
                ),
              ),
            ),
          const SliverToBoxAdapter(child: SizedBox(height: 20)),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    final now = DateFormat('HH:mm - dd/MM/yyyy').format(DateTime.now());
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
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
                child: const Icon(Icons.show_chart, color: Colors.white, size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Bảng Giá Vàng Hôm Nay',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Cập nhật lúc: $now',
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.grey[400],
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: _loadPrices,
                icon: const Icon(Icons.refresh, color: Colors.amber),
                tooltip: 'Làm mới',
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: Colors.amber.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: Colors.amber.withValues(alpha: 0.2)),
            ),
            child: Row(
              children: [
                Icon(Icons.info_outline, size: 16, color: Colors.amber[300]),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Kéo xuống để làm mới giá • Đơn vị: VNĐ/Lượng',
                    style: TextStyle(fontSize: 12, color: Colors.amber[300]),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPriceCard(GoldPrice price) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            const Color(0xFF1E1E2E),
            const Color(0xFF2A2A3E),
          ],
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: Colors.amber.withValues(alpha: 0.15),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.2),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [Colors.amber[400]!, Colors.amber[700]!],
                    ),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.monetization_on,
                      color: Colors.white, size: 22),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    price.type,
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _buildPriceColumn(
                    'MUA VÀO',
                    _currencyFormat.format(price.buyPrice),
                    Colors.greenAccent[400]!,
                    Icons.arrow_downward,
                  ),
                ),
                Container(
                  width: 1,
                  height: 50,
                  color: Colors.grey[700],
                ),
                Expanded(
                  child: _buildPriceColumn(
                    'BÁN RA',
                    _currencyFormat.format(price.sellPrice),
                    Colors.redAccent[200]!,
                    Icons.arrow_upward,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPriceColumn(
      String label, String value, Color color, IconData icon) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 14, color: color),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: color,
                letterSpacing: 1,
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        FittedBox(
          child: Text(
            value,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
        ),
      ],
    );
  }
}
