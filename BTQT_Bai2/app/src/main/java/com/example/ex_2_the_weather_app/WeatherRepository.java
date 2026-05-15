package com.example.ex_2_the_weather_app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherRepository {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "cd85c9c01e5bf1e8a77d1b156ce598e9";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(WeatherData data);

        void onError(String message);
    }

    public void fetchWeather(double latitude, double longitude, boolean metric, Callback callback) {
        HttpUrl currentUrl = buildUrl("weather", latitude, longitude, metric);
        HttpUrl forecastUrl = buildUrl("forecast", latitude, longitude, metric);

        final JSONObject[] currentHolder = new JSONObject[1];
        final JSONObject[] forecastHolder = new JSONObject[1];
        final AtomicInteger pending = new AtomicInteger(2);
        final AtomicBoolean failed = new AtomicBoolean(false);

        fetchJson(currentUrl, new JsonCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                currentHolder[0] = jsonObject;
                if (pending.decrementAndGet() == 0 && !failed.get()) {
                    deliverResult(currentHolder[0], forecastHolder[0], callback);
                }
            }

            @Override
            public void onError(String message) {
                if (failed.compareAndSet(false, true)) {
                    postError(callback, message);
                }
            }
        });

        fetchJson(forecastUrl, new JsonCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                forecastHolder[0] = jsonObject;
                if (pending.decrementAndGet() == 0 && !failed.get()) {
                    deliverResult(currentHolder[0], forecastHolder[0], callback);
                }
            }

            @Override
            public void onError(String message) {
                if (failed.compareAndSet(false, true)) {
                    postError(callback, message);
                }
            }
        });
    }

    private HttpUrl buildUrl(String endpoint, double latitude, double longitude, boolean metric) {
        return HttpUrl.parse(BASE_URL + endpoint).newBuilder()
                .addQueryParameter("lat", String.valueOf(latitude))
                .addQueryParameter("lon", String.valueOf(longitude))
                .addQueryParameter("appid", API_KEY)
                .addQueryParameter("units", metric ? "metric" : "imperial")
                .addQueryParameter("lang", "vi")
                .build();
    }

    private void fetchJson(HttpUrl url, JsonCallback callback) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull IOException e) {
                callback.onError("Lỗi mạng: " + e.getMessage());
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull Response response) throws IOException {
                try (Response body = response) {
                    if (!body.isSuccessful() || body.body() == null) {
                        callback.onError("Phản hồi không hợp lệ từ máy chủ.");
                        return;
                    }
                    String json = body.body().string();
                    callback.onSuccess(new JSONObject(json));
                } catch (JSONException e) {
                    callback.onError("Không đọc được dữ liệu thời tiết.");
                }
            }
        });
    }

    private void deliverResult(JSONObject currentJson, JSONObject forecastJson, Callback callback) {
        try {
            WeatherData data = new WeatherData();
            parseCurrent(currentJson, data);
            parseForecast(forecastJson, data);
            mainHandler.post(() -> callback.onSuccess(data));
        } catch (JSONException | ParseException e) {
            postError(callback, "Không xử lý được dữ liệu thời tiết.");
        }
    }

    private void parseCurrent(JSONObject currentJson, WeatherData data) throws JSONException {
        data.setLocationName(currentJson.optString("name", "Vị trí hiện tại"));
        JSONObject main = currentJson.getJSONObject("main");
        data.setCurrentTemp(main.optDouble("temp", 0.0));
        data.setFeelsLike(main.optDouble("feels_like", 0.0));
        data.setHighTemp(main.optDouble("temp_max", 0.0));
        data.setLowTemp(main.optDouble("temp_min", 0.0));
        data.setHumidity(main.optInt("humidity", 0));
        JSONObject wind = currentJson.optJSONObject("wind");
        if (wind != null) {
            data.setWindSpeed(wind.optDouble("speed", 0.0));
        }
        JSONArray weatherArray = currentJson.optJSONArray("weather");
        if (weatherArray != null && weatherArray.length() > 0) {
            JSONObject weather = weatherArray.getJSONObject(0);
            String description = weather.optString("description", "");
            data.setCurrentCondition(capitalize(description));
            data.setCurrentIconCode(weather.optString("icon", ""));
        }
    }

    private void parseForecast(JSONObject forecastJson, WeatherData data) throws JSONException, ParseException {
        JSONArray list = forecastJson.getJSONArray("list");
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE dd/MM", new Locale("vi", "VN"));
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Map<String, DailyAccumulator> dailyMap = new LinkedHashMap<>();

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            String dateText = item.optString("dt_txt", null);
            if (dateText == null) {
                continue;
            }
            Date date = inputFormat.parse(dateText);
            if (date == null) {
                continue;
            }

            JSONObject main = item.getJSONObject("main");
            JSONArray weatherArray = item.optJSONArray("weather");
            String description = "";
            String iconCode = "";
            if (weatherArray != null && weatherArray.length() > 0) {
                JSONObject weather = weatherArray.getJSONObject(0);
                description = capitalize(weather.optString("description", ""));
                iconCode = weather.optString("icon", "");
            }

            if (data.getHourlyForecast().size() < 8) {
                data.getHourlyForecast().add(new WeatherForecastItem(
                        hourFormat.format(date),
                        formatTemperature(main.optDouble("temp", 0.0)),
                        description,
                        iconCode
                ));
            }

            String dayKey = keyFormat.format(date);
            DailyAccumulator accumulator = dailyMap.get(dayKey);
            if (accumulator == null) {
                accumulator = new DailyAccumulator(dayFormat.format(date), description, iconCode);
                dailyMap.put(dayKey, accumulator);
            }
            accumulator.include(main.optDouble("temp_min", 0.0), main.optDouble("temp_max", 0.0), description, iconCode);
        }

        for (DailyAccumulator accumulator : dailyMap.values()) {
            data.getDailyForecast().add(new WeatherForecastItem(
                    accumulator.label,
                    formatTemperatureRange(accumulator.minTemp, accumulator.maxTemp),
                    accumulator.description,
                    accumulator.iconCode
            ));
            if (data.getDailyForecast().size() >= 5) {
                break;
            }
        }
    }

    private String formatTemperature(double value) {
        return Math.round(value) + "°";
    }

    private String formatTemperatureRange(double min, double max) {
        return Math.round(max) + "° / " + Math.round(min) + "°";
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmed.substring(1);
    }

    private void postError(Callback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private interface JsonCallback {
        void onSuccess(JSONObject jsonObject);

        void onError(String message);
    }

    private static class DailyAccumulator {
        private final String label;
        private String description;
        private String iconCode;
        private double minTemp;
        private double maxTemp;

        private DailyAccumulator(String label, String description, String iconCode) {
            this.label = label;
            this.description = description;
            this.iconCode = iconCode;
            this.minTemp = Double.MAX_VALUE;
            this.maxTemp = -Double.MAX_VALUE;
        }

        private void include(double tempMin, double tempMax, String description, String iconCode) {
            this.minTemp = Math.min(this.minTemp, tempMin);
            this.maxTemp = Math.max(this.maxTemp, tempMax);
            if (description != null && !description.isEmpty()) {
                this.description = description;
            }
            if (iconCode != null && !iconCode.isEmpty()) {
                this.iconCode = iconCode;
            }
        }
    }
}