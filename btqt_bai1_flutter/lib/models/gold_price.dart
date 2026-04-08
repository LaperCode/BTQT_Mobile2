class GoldPrice {
  final String type;
  final double buyPrice; // VND per lượng
  final double sellPrice; // VND per lượng
  final String unit;
  final DateTime lastUpdated;

  GoldPrice({
    required this.type,
    required this.buyPrice,
    required this.sellPrice,
    this.unit = 'Lượng',
    required this.lastUpdated,
  });

  /// Price per gram (1 lượng = 37.5 gram)
  double get buyPricePerGram => buyPrice / 37.5;
  double get sellPricePerGram => sellPrice / 37.5;

  /// Price per chỉ (1 lượng = 10 chỉ)
  double get buyPricePerChi => buyPrice / 10;
  double get sellPricePerChi => sellPrice / 10;

  /// Price per troy ounce (1 oz = 31.1035 gram)
  double get buyPricePerOunce => buyPrice * (31.1035 / 37.5);
  double get sellPricePerOunce => sellPrice * (31.1035 / 37.5);
}
