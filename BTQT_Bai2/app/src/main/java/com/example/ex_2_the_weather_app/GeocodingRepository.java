package com.example.ex_2_the_weather_app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeocodingRepository {
    private static final String NOMINATIM_API = "https://nominatim.openstreetmap.org/reverse";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface GeocodingCallback {
        void onSuccess(String address);
        void onError(String message);
    }

    public void reverseGeocode(double latitude, double longitude, GeocodingCallback callback) {
        HttpUrl url = HttpUrl.parse(NOMINATIM_API).newBuilder()
                .addQueryParameter("lat", String.valueOf(latitude))
                .addQueryParameter("lon", String.valueOf(longitude))
                .addQueryParameter("format", "json")
                .addQueryParameter("zoom", "10")
                .addQueryParameter("addressdetails", "1")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "WeatherApp/1.0")
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("GeocodingRepository", "Reverse geocoding failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Lỗi lấy địa chỉ: " + e.getMessage()));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (Response body = response) {
                    if (!body.isSuccessful() || body.body() == null) {
                        Log.e("GeocodingRepository", "API error: " + body.code());
                        mainHandler.post(() -> callback.onError("Phản hồi không hợp lệ."));
                        return;
                    }
                    String json = body.body().string();
                    Log.d("GeocodingRepository", "API response: " + json);
                    JSONObject jsonObject = new JSONObject(json);
                    String address = parseAddress(jsonObject);
                    Log.d("GeocodingRepository", "Parsed address: " + address);
                    mainHandler.post(() -> callback.onSuccess(address));
                } catch (JSONException e) {
                    Log.e("GeocodingRepository", "JSON parsing error: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Lỗi phân tích địa chỉ."));
                }
            }
        });
    }

    private String parseAddress(JSONObject response) throws JSONException {
        JSONObject address = response.optJSONObject("address");
        if (address == null) {
            return response.optString("display_name", "Vị trí không xác định");
        }

        StringBuilder result = new StringBuilder();

        String village = address.optString("village", null);
        String town = address.optString("town", null);
        String city = address.optString("city", null);
        String county = address.optString("county", null);
        String state = address.optString("state", null);
        String country = address.optString("country", null);

        if (village != null && !village.isEmpty()) {
            result.append(village);
        } else if (town != null && !town.isEmpty()) {
            result.append(town);
        } else if (city != null && !city.isEmpty()) {
            result.append(city);
        }

        if (county != null && !county.isEmpty() && !county.equals(result.toString())) {
            if (result.length() > 0) result.append(", ");
            result.append(county);
        }

        if (state != null && !state.isEmpty() && !state.equals(result.toString())) {
            if (result.length() > 0) result.append(", ");
            result.append(state);
        }

        if (country != null && !country.isEmpty()) {
            if (result.length() > 0) result.append(", ");
            result.append(country);
        }

        return result.length() > 0 ? result.toString() : response.optString("display_name", "Vị trí không xác định");
    }
}
