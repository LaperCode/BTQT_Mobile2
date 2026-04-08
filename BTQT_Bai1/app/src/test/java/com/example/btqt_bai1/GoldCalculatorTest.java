package com.example.btqt_bai1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GoldCalculatorTest {

    @Test
    public void convertAmountToUsd_gramUnit() {
        double priceOunceUsd = 2000.0;
        double amountGram = 10.0;
        double expectedUsd = amountGram * (priceOunceUsd / GoldCalculator.GRAM_PER_OUNCE);
        double actualUsd = GoldCalculator.convertAmountToUsd(amountGram, "Gram (g)", priceOunceUsd);
        assertEquals(expectedUsd, actualUsd, 0.0001);
    }

    @Test
    public void convertOunceUsdToLuongVnd() {
        double priceOunceUsd = 2100.0;
        double rate = 25000.0;
        double expected = (priceOunceUsd / GoldCalculator.LUONG_PER_OUNCE) * rate;
        double actual = GoldCalculator.convertOunceUsdToLuongVnd(priceOunceUsd, rate);
        assertEquals(expected, actual, 0.0001);
    }
}
