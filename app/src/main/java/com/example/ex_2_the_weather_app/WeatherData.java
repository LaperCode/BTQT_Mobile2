package com.example.ex_2_the_weather_app;

import java.util.ArrayList;
import java.util.List;

public class WeatherData {
    private String locationName;
    private String currentCondition;
    private String currentIconCode;
    private double currentTemp;
    private double feelsLike;
    private double highTemp;
    private double lowTemp;
    private int humidity;
    private double windSpeed;
    private String alertMessage;
    private final List<WeatherForecastItem> hourlyForecast = new ArrayList<>();
    private final List<WeatherForecastItem> dailyForecast = new ArrayList<>();

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getCurrentCondition() {
        return currentCondition;
    }

    public void setCurrentCondition(String currentCondition) {
        this.currentCondition = currentCondition;
    }

    public String getCurrentIconCode() {
        return currentIconCode;
    }

    public void setCurrentIconCode(String currentIconCode) {
        this.currentIconCode = currentIconCode;
    }

    public double getCurrentTemp() {
        return currentTemp;
    }

    public void setCurrentTemp(double currentTemp) {
        this.currentTemp = currentTemp;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(double feelsLike) {
        this.feelsLike = feelsLike;
    }

    public double getHighTemp() {
        return highTemp;
    }

    public void setHighTemp(double highTemp) {
        this.highTemp = highTemp;
    }

    public double getLowTemp() {
        return lowTemp;
    }

    public void setLowTemp(double lowTemp) {
        this.lowTemp = lowTemp;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public List<WeatherForecastItem> getHourlyForecast() {
        return hourlyForecast;
    }

    public List<WeatherForecastItem> getDailyForecast() {
        return dailyForecast;
    }
}