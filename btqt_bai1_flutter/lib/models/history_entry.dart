class HistoryEntry {
  final int? id;
  final String goldType;
  final double quantity;
  final String unit;
  final double pricePerUnit;
  final double totalValue;
  final DateTime timestamp;

  HistoryEntry({
    this.id,
    required this.goldType,
    required this.quantity,
    required this.unit,
    required this.pricePerUnit,
    required this.totalValue,
    required this.timestamp,
  });

  Map<String, dynamic> toMap() {
    return {
      'goldType': goldType,
      'quantity': quantity,
      'unit': unit,
      'pricePerUnit': pricePerUnit,
      'totalValue': totalValue,
      'timestamp': timestamp.toIso8601String(),
    };
  }

  factory HistoryEntry.fromMap(Map<String, dynamic> map) {
    return HistoryEntry(
      id: map['id'] as int?,
      goldType: map['goldType'] as String,
      quantity: (map['quantity'] as num).toDouble(),
      unit: map['unit'] as String,
      pricePerUnit: (map['pricePerUnit'] as num).toDouble(),
      totalValue: (map['totalValue'] as num).toDouble(),
      timestamp: DateTime.parse(map['timestamp'] as String),
    );
  }
}
