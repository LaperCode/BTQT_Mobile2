package com.example.btqt_bai1;

import java.util.Locale;

public final class GoldCalculator {

    public static final double GRAM_PER_OUNCE = 31.1034768;
    public static final double LUONG_PER_OUNCE = 0.8294;
    public static final double CHI_PER_OUNCE = 8.294;

    private GoldCalculator() {
    }

    public static double getPremiumMultiplier(String goldType) {
        if (goldType == null) {
            return 1.0;
        }
        switch (goldType) {
            case "Vàng SJC":
                return 1.25;
            case "Vàng PNJ":
                return 1.15;
            case "Vàng nhẫn 9999":
                return 1.10;
            case "Vàng Thế Giới":
            default:
                return 1.0;
        }
    }

    public static double convertAmountToUsd(double amount, String unit, double priceOunceUsd) {
        if (unit == null) {
            return 0.0;
        }
        switch (unit) {
            case "Ounce (oz)":
                return amount * priceOunceUsd;
            case "Gram (g)":
                return amount * (priceOunceUsd / GRAM_PER_OUNCE);
            case "Lượng (Cây)":
                return amount * (priceOunceUsd / LUONG_PER_OUNCE);
            case "Chỉ":
                return amount * (priceOunceUsd / CHI_PER_OUNCE);
            default:
                return 0.0;
        }
    }

    public static double convertOunceUsdToLuongVnd(double priceOunceUsd, double usdToVndRate) {
        return (priceOunceUsd / LUONG_PER_OUNCE) * usdToVndRate;
    }

    public static String formatMoneyVnd(double value) {
        return String.format(Locale.getDefault(), "%,.0fđ", value);
    }
}
