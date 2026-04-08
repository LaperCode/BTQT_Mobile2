import 'dart:convert';
import 'dart:math';
import 'package:http/http.dart' as http;
import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../models/gold_price.dart';

class GoldApiService {
  static String get _apiKey => dotenv.env['METALPRICE_API_KEY'] ?? '';
  static const String _baseUrl = 'https://api.metalpriceapi.com/v1';

  // Vietnamese gold premiums over world price (approximate)
  static const Map<String, double> _buyPremiumPercent = {
    'Vàng SJC': 1.15, // SJC has ~15% premium
    'Vàng PNJ': 1.12,
    'Vàng Nhẫn 24K': 1.10,
    'Vàng 18K': 0.83, // 18K = 75% purity, slight premium
    'Vàng 14K': 0.65,
  };

  static const Map<String, double> _sellSpreadPercent = {
    'Vàng SJC': 0.995, // sell ~0.5% less than buy
    'Vàng PNJ': 0.993,
    'Vàng Nhẫn 24K': 0.990,
    'Vàng 18K': 0.988,
    'Vàng 14K': 0.985,
  };

  /// Fetch world gold price (XAU) in VND from MetalpriceAPI,
  /// then derive Vietnamese gold prices with market premiums.
  Future<List<GoldPrice>> fetchGoldPrices() async {
    try {
      if (_apiKey.isEmpty) {
        return _getFallbackPrices();
      }
      final url = Uri.parse(
        '$_baseUrl/latest?api_key=$_apiKey&base=USD&currencies=XAU,VND',
      );
      final response = await http.get(url);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['success'] == true) {
          final rates = data['rates'] as Map<String, dynamic>;

          // XAU rate is per 1 USD in troy ounces
          // VND rate is per 1 USD in VND
          final xauRate = (rates['XAU'] as num).toDouble();
          final vndRate = (rates['VND'] as num).toDouble();

          // Price of 1 troy ounce of gold in VND
          final goldPricePerOunceVND = vndRate / xauRate;

          // 1 troy ounce = 31.1035 gram
          // 1 lượng = 37.5 gram
          final goldPricePerLuong = goldPricePerOunceVND * (37.5 / 31.1035);

          final now = DateTime.now();

          return _buildGoldPrices(goldPricePerLuong, now);
        }
      }
      // Fallback if API fails
      return _getFallbackPrices();
    } catch (e) {
      return _getFallbackPrices();
    }
  }

  List<GoldPrice> _buildGoldPrices(double basePrice, DateTime now) {
    final prices = <GoldPrice>[];
    for (final entry in _buyPremiumPercent.entries) {
      final buyPrice = basePrice * entry.value;
      final sellPrice = buyPrice * (_sellSpreadPercent[entry.key] ?? 0.99);
      // Round to nearest 10,000 VND for realism
      prices.add(
        GoldPrice(
          type: entry.key,
          buyPrice: (buyPrice / 10000).round() * 10000.0,
          sellPrice: (sellPrice / 10000).round() * 10000.0,
          lastUpdated: now,
        ),
      );
    }
    return prices;
  }

  /// Get 7-day simulated history based on current price
  Future<List<Map<String, dynamic>>> fetch7DayHistory() async {
    final prices = await fetchGoldPrices();
    if (prices.isEmpty) return [];

    final random = Random(DateTime.now().day);
    final history = <Map<String, dynamic>>[];

    for (int i = 6; i >= 0; i--) {
      final date = DateTime.now().subtract(Duration(days: i));
      final dayPrices = <String, double>{};
      for (final p in prices) {
        // Random fluctuation ±2%
        final factor = 0.98 + random.nextDouble() * 0.04;
        dayPrices[p.type] = (p.sellPrice * factor / 10000).round() * 10000.0;
      }
      history.add({'date': date, 'prices': dayPrices});
    }

    return history;
  }

  /// Fallback prices when API is unavailable
  List<GoldPrice> _getFallbackPrices() {
    final now = DateTime.now();
    // Approximate prices based on March 2026 market
    const basePrice = 95000000.0; // ~95 triệu/lượng base
    return _buildGoldPrices(basePrice, now);
  }
}
