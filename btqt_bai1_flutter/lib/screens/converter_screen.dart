import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/gold_price.dart';
import '../models/history_entry.dart';
import '../services/gold_api_service.dart';
import '../services/database_helper.dart';

class ConverterScreen extends StatefulWidget {
  const ConverterScreen({super.key});

  @override
  State<ConverterScreen> createState() => _ConverterScreenState();
}

class _ConverterScreenState extends State<ConverterScreen> {
  final GoldApiService _apiService = GoldApiService();
  final TextEditingController _quantityController = TextEditingController();
  final _currencyFormat = NumberFormat.currency(
    locale: 'vi_VN',
    symbol: '₫',
    decimalDigits: 0,
  );

  List<GoldPrice> _prices = [];
  bool _isLoading = true;
  String _selectedGoldType = 'Vàng SJC';
  String _selectedUnit = 'Lượng';
  double _result = 0;
  bool _useBuyPrice = true;

  final List<String> _units = ['Lượng', 'Chỉ', 'Gram', 'Ounce'];

  @override
  void initState() {
    super.initState();
    _quantityController.addListener(_calculate);
    _loadPrices();
  }

  @override
  void dispose() {
    _quantityController.dispose();
    super.dispose();
  }

  Future<void> _loadPrices() async {
    setState(() => _isLoading = true);
    try {
      final prices = await _apiService.fetchGoldPrices();
      setState(() {
        _prices = prices;
        _isLoading = false;
      });
      _calculate();
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  void _calculate() {
    if (_prices.isEmpty) return;
    final quantity = double.tryParse(_quantityController.text) ?? 0;
    final price = _prices.firstWhere(
      (p) => p.type == _selectedGoldType,
      orElse: () => _prices.first,
    );

    double pricePerUnit;
    if (_useBuyPrice) {
      switch (_selectedUnit) {
        case 'Chỉ':
          pricePerUnit = price.buyPricePerChi;
          break;
        case 'Gram':
          pricePerUnit = price.buyPricePerGram;
          break;
        case 'Ounce':
          pricePerUnit = price.buyPricePerOunce;
          break;
        default:
          pricePerUnit = price.buyPrice;
      }
    } else {
      switch (_selectedUnit) {
        case 'Chỉ':
          pricePerUnit = price.sellPricePerChi;
          break;
        case 'Gram':
          pricePerUnit = price.sellPricePerGram;
          break;
        case 'Ounce':
          pricePerUnit = price.sellPricePerOunce;
          break;
        default:
          pricePerUnit = price.sellPrice;
      }
    }

    setState(() {
      _result = quantity * pricePerUnit;
    });
  }

  Future<void> _saveToHistory() async {
    if (_result <= 0) return;
    final quantity = double.tryParse(_quantityController.text) ?? 0;
    if (quantity <= 0) return;

    final price = _prices.firstWhere(
      (p) => p.type == _selectedGoldType,
      orElse: () => _prices.first,
    );

    double pricePerUnit;
    if (_useBuyPrice) {
      switch (_selectedUnit) {
        case 'Chỉ':
          pricePerUnit = price.buyPricePerChi;
          break;
        case 'Gram':
          pricePerUnit = price.buyPricePerGram;
          break;
        case 'Ounce':
          pricePerUnit = price.buyPricePerOunce;
          break;
        default:
          pricePerUnit = price.buyPrice;
      }
    } else {
      switch (_selectedUnit) {
        case 'Chỉ':
          pricePerUnit = price.sellPricePerChi;
          break;
        case 'Gram':
          pricePerUnit = price.sellPricePerGram;
          break;
        case 'Ounce':
          pricePerUnit = price.sellPricePerOunce;
          break;
        default:
          pricePerUnit = price.sellPrice;
      }
    }

    final entry = HistoryEntry(
      goldType: _selectedGoldType,
      quantity: quantity,
      unit: _selectedUnit,
      pricePerUnit: pricePerUnit,
      totalValue: _result,
      timestamp: DateTime.now(),
    );

    await DatabaseHelper.instance.insertHistory(entry);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Row(
            children: [
              Icon(Icons.check_circle, color: Colors.white, size: 20),
              SizedBox(width: 8),
              Text('Đã lưu vào lịch sử!'),
            ],
          ),
          backgroundColor: Colors.green[700],
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: Colors.amber),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Title section
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
                child: const Icon(
                  Icons.currency_exchange,
                  color: Colors.white,
                  size: 24,
                ),
              ),
              const SizedBox(width: 14),
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Quy Đổi Vàng',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  SizedBox(height: 2),
                  Text(
                    'Chuyển đổi vàng sang tiền VNĐ',
                    style: TextStyle(fontSize: 13, color: Colors.grey),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 24),

          // Gold type selector
          _buildSectionLabel('Loại vàng'),
          const SizedBox(height: 8),
          _buildDropdown(
            value: _selectedGoldType,
            items: _prices.map((p) => p.type).toList(),
            onChanged: (v) {
              setState(() => _selectedGoldType = v!);
              _calculate();
            },
          ),
          const SizedBox(height: 20),

          // Unit selector
          _buildSectionLabel('Đơn vị'),
          const SizedBox(height: 8),
          Row(
            children: _units.map((unit) {
              final isSelected = _selectedUnit == unit;
              return Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: GestureDetector(
                    onTap: () {
                      setState(() => _selectedUnit = unit);
                      _calculate();
                    },
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      decoration: BoxDecoration(
                        gradient: isSelected
                            ? LinearGradient(
                                colors: [
                                  Colors.amber[600]!,
                                  Colors.orange[700]!,
                                ],
                              )
                            : null,
                        color: isSelected ? null : const Color(0xFF1E1E2E),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: isSelected
                              ? Colors.transparent
                              : Colors.grey[700]!,
                        ),
                      ),
                      child: Center(
                        child: Text(
                          unit,
                          style: TextStyle(
                            fontWeight: FontWeight.w600,
                            color: isSelected ? Colors.white : Colors.grey[400],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),

          // Price type toggle
          _buildSectionLabel('Giá tham khảo'),
          const SizedBox(height: 8),
          Row(
            children: [
              _buildToggleButton('Giá Mua', _useBuyPrice, () {
                setState(() => _useBuyPrice = true);
                _calculate();
              }),
              const SizedBox(width: 10),
              _buildToggleButton('Giá Bán', !_useBuyPrice, () {
                setState(() => _useBuyPrice = false);
                _calculate();
              }),
            ],
          ),
          const SizedBox(height: 20),

          // Quantity input
          _buildSectionLabel('Số lượng'),
          const SizedBox(height: 8),
          Container(
            decoration: BoxDecoration(
              color: const Color(0xFF1E1E2E),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: Colors.grey[700]!),
            ),
            child: TextField(
              controller: _quantityController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
              decoration: InputDecoration(
                hintText: '0',
                hintStyle: TextStyle(color: Colors.grey[600]),
                prefixIcon: Icon(Icons.scale, color: Colors.amber[400]),
                suffixText: _selectedUnit,
                suffixStyle: TextStyle(
                  color: Colors.amber[300],
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
                border: InputBorder.none,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 18,
                ),
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Result card
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  Colors.amber[700]!.withValues(alpha: 0.2),
                  Colors.orange[800]!.withValues(alpha: 0.1),
                ],
              ),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: Colors.amber.withValues(alpha: 0.3)),
            ),
            child: Column(
              children: [
                Text(
                  'GIÁ TRỊ QUY ĐỔI',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: Colors.amber[300],
                    letterSpacing: 2,
                  ),
                ),
                const SizedBox(height: 12),
                FittedBox(
                  child: Text(
                    _currencyFormat.format(_result),
                    style: const TextStyle(
                      fontSize: 36,
                      fontWeight: FontWeight.w800,
                      color: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  '${_quantityController.text.isEmpty ? "0" : _quantityController.text} $_selectedUnit $_selectedGoldType',
                  style: TextStyle(fontSize: 14, color: Colors.grey[400]),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Save button
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton(
              onPressed: _result > 0 ? _saveToHistory : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.amber[700],
                disabledBackgroundColor: Colors.grey[800],
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
                elevation: 4,
              ),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.save_alt, size: 22),
                  SizedBox(width: 10),
                  Text(
                    'Lưu vào lịch sử',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildSectionLabel(String text) {
    return Text(
      text,
      style: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w600,
        color: Colors.grey[400],
        letterSpacing: 0.5,
      ),
    );
  }

  Widget _buildDropdown({
    required String value,
    required List<String> items,
    required ValueChanged<String?> onChanged,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
        color: const Color(0xFF1E1E2E),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.grey[700]!),
      ),
      child: DropdownButton<String>(
        value: value,
        isExpanded: true,
        underline: const SizedBox(),
        dropdownColor: const Color(0xFF1E1E2E),
        icon: Icon(Icons.keyboard_arrow_down, color: Colors.amber[400]),
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: Colors.white,
        ),
        items: items.map((item) {
          return DropdownMenuItem(
            value: item,
            child: Row(
              children: [
                Icon(Icons.circle, size: 8, color: Colors.amber[400]),
                const SizedBox(width: 10),
                Text(item),
              ],
            ),
          );
        }).toList(),
        onChanged: onChanged,
      ),
    );
  }

  Widget _buildToggleButton(String label, bool isActive, VoidCallback onTap) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          padding: const EdgeInsets.symmetric(vertical: 14),
          decoration: BoxDecoration(
            gradient: isActive
                ? LinearGradient(
                    colors: [Colors.amber[600]!, Colors.orange[700]!],
                  )
                : null,
            color: isActive ? null : const Color(0xFF1E1E2E),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: isActive ? Colors.transparent : Colors.grey[700]!,
            ),
          ),
          child: Center(
            child: Text(
              label,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: isActive ? Colors.white : Colors.grey[400],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
