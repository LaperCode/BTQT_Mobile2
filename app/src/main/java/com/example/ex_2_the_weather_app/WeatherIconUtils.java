package com.example.ex_2_the_weather_app;

import java.util.Locale;

public final class WeatherIconUtils {
    private WeatherIconUtils() {
    }

    public static String emojiFor(String iconCode, String description) {
        if (iconCode == null) {
            iconCode = "";
        }
        if (description == null) {
            description = "";
        }

        String lower = description.toLowerCase(Locale.getDefault());
        if (iconCode.startsWith("01")) {
            return "☀";
        }
        if (iconCode.startsWith("02")) {
            return "🌤";
        }
        if (iconCode.startsWith("03") || iconCode.startsWith("04")) {
            return "☁";
        }
        if (iconCode.startsWith("09") || lower.contains("mưa")) {
            return "🌧";
        }
        if (iconCode.startsWith("10")) {
            return "🌦";
        }
        if (iconCode.startsWith("11") || lower.contains("dông")) {
            return "⛈";
        }
        if (iconCode.startsWith("13")) {
            return "❄";
        }
        if (iconCode.startsWith("50")) {
            return "🌫";
        }
        return "🌡";
    }

    public static String buildAlertMessage(String description, double temp, double windSpeed) {
        String text = description == null ? "" : description.toLowerCase(Locale.getDefault());
        if (temp >= 38) {
            return "Cảnh báo nắng nóng: nhiệt độ hiện tại đang rất cao.";
        }
        if (temp <= 0) {
            return "Cảnh báo lạnh: nhiệt độ hiện tại đang xuống thấp.";
        }
        if (windSpeed >= 12) {
            return "Cảnh báo gió mạnh: tốc độ gió đang vượt ngưỡng an toàn.";
        }
        if (text.contains("dông") || text.contains("bão") || text.contains("sấm") || text.contains("storm")) {
            return "Cảnh báo thời tiết xấu: có khả năng dông bão hoặc mưa lớn.";
        }
        if (text.contains("mưa lớn") || text.contains("heavy rain")) {
            return "Cảnh báo mưa lớn: nên hạn chế di chuyển ngoài trời.";
        }
        return null;
    }
}