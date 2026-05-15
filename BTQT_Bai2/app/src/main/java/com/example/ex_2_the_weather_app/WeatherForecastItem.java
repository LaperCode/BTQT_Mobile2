package com.example.ex_2_the_weather_app;

public class WeatherForecastItem {
    private final String label;
    private final String value;
    private final String description;
    private final String iconCode;

    public WeatherForecastItem(String label, String value, String description, String iconCode) {
        this.label = label;
        this.value = value;
        this.description = description;
        this.iconCode = iconCode;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getIconCode() {
        return iconCode;
    }
}