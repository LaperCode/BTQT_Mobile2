package com.example.ex_2_the_weather_app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "weather_prefs";
    private static final String KEY_IS_METRIC = "is_metric";
    private static final String CHANNEL_ID = "weather_alerts";
    private static final String OPEN_WEATHER_API_KEY = "cd85c9c01e5bf1e8a77d1b156ce598e9";

    private TextView tvLocation;
    private TextView tvCurrentEmoji;
    private TextView tvCurrentTemp;
    private TextView tvCurrentCondition;
    private TextView tvCurrentDetails;
    private TextView tvStatus;
    private TextView tvAlert;
    private MaterialCardView cardAlert;
    private ProgressBar progressLoading;
    private Button btnRefresh;
    private Button btnToggleUnit;
    private Button btnLayerCloud;
    private Button btnLayerTemp;
    private Button btnLayerRain;
    private WebView weatherWebView;
    private WeatherForecastAdapter hourlyAdapter;
    private WeatherForecastAdapter dailyAdapter;
    private WeatherRepository weatherRepository;
    private SharedPreferences preferences;
    private boolean isMetric;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private String currentMapLayer = "clouds_new";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isMetric = preferences.getBoolean(KEY_IS_METRIC, true);
        weatherRepository = new WeatherRepository();

        bindViews();
        setupLists();
        setupPermissionLauncher();
        setupActions();
        updateUnitButton();
        initMap();
        requestWeatherPermissions();
    }

    private void bindViews() {
        tvLocation = findViewById(R.id.tvLocation);
        tvCurrentEmoji = findViewById(R.id.tvCurrentEmoji);
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCurrentCondition = findViewById(R.id.tvCurrentCondition);
        tvCurrentDetails = findViewById(R.id.tvCurrentDetails);
        tvStatus = findViewById(R.id.tvStatus);
        tvAlert = findViewById(R.id.tvAlert);
        cardAlert = findViewById(R.id.cardAlert);
        weatherWebView = findViewById(R.id.weatherWebView);
        progressLoading = findViewById(R.id.progressLoading);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnToggleUnit = findViewById(R.id.btnToggleUnit);
        btnLayerCloud = findViewById(R.id.btnLayerCloud);
        btnLayerTemp = findViewById(R.id.btnLayerTemp);
        btnLayerRain = findViewById(R.id.btnLayerRain);
    }

    private void initMap() {
        if (weatherWebView == null) {
            return;
        }

        WebSettings settings = weatherWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        weatherWebView.setBackgroundColor(0x00000000);

        String html = "<!DOCTYPE html>"
                + "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<style>html,body,#map{height:100%;margin:0;padding:0;} .leaflet-control-attribution{font-size:9px;}</style>"
                + "</head><body><div id='map'></div>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<script>"
                + "var map=L.map('map').setView([10.8231,106.6297],8);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);"
                + "var weatherLayer=null;"
                + "function setWeatherLayer(layer){"
                + " if(weatherLayer){map.removeLayer(weatherLayer);}"
                + " weatherLayer=L.tileLayer('https://tile.openweathermap.org/map/'+layer+'/{z}/{x}/{y}.png?appid=" + OPEN_WEATHER_API_KEY + "',{opacity:0.8,maxZoom:18});"
                + " weatherLayer.addTo(map);"
                + "}"
                + "function moveTo(lat,lon){map.setView([lat,lon],9);}"
                + "setWeatherLayer('" + currentMapLayer + "');"
                + "</script></body></html>";

        weatherWebView.loadDataWithBaseURL("https://localhost/", html, "text/html", "utf-8", null);
    }

    private void setupLists() {
        RecyclerView rvHourly = findViewById(R.id.rvHourly);
        RecyclerView rvDaily = findViewById(R.id.rvDaily);

        hourlyAdapter = new WeatherForecastAdapter(true);
        dailyAdapter = new WeatherForecastAdapter(false);

        rvHourly.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvHourly.setAdapter(hourlyAdapter);

        rvDaily.setLayoutManager(new LinearLayoutManager(this));
        rvDaily.setAdapter(dailyAdapter);
        rvDaily.setNestedScrollingEnabled(false);
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean locationGranted = isPermissionGranted(result, Manifest.permission.ACCESS_FINE_LOCATION)
                            || isPermissionGranted(result, Manifest.permission.ACCESS_COARSE_LOCATION);
                    if (locationGranted) {
                        loadWeather();
                    } else {
                        showStatus(getString(R.string.weather_permission_required), true);
                    }
                }
        );
    }

    private void setupActions() {
        btnRefresh.setOnClickListener(v -> requestWeatherPermissions());
        btnToggleUnit.setOnClickListener(v -> {
            isMetric = !isMetric;
            preferences.edit().putBoolean(KEY_IS_METRIC, isMetric).apply();
            updateUnitButton();
            requestWeatherPermissions();
        });

        btnLayerCloud.setOnClickListener(v -> {
            currentMapLayer = "clouds_new";
            applyWeatherOverlay();
        });

        btnLayerTemp.setOnClickListener(v -> {
            currentMapLayer = "temp_new";
            applyWeatherOverlay();
        });

        btnLayerRain.setOnClickListener(v -> {
            currentMapLayer = "precipitation_new";
            applyWeatherOverlay();
        });
    }

    private void updateUnitButton() {
        btnToggleUnit.setText(isMetric ? "°C" : "°F");
    }

    private void requestWeatherPermissions() {
        if (hasLocationPermission()) {
            loadWeather();
            return;
        }

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
        permissionLauncher.launch(permissions);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isPermissionGranted(@NonNull Map<String, Boolean> result, String permission) {
        Boolean granted = result.get(permission);
        return granted != null && granted;
    }

    private void loadWeather() {
        setLoading(true);
        showStatus(null, false);

        Location location = getBestLastKnownLocation();
        if (location != null) {
            fetchWeather(location);
            return;
        }

        setLoading(false);
        showStatus("Không lấy được vị trí hiện tại. Hãy bật GPS rồi thử lại.", true);
    }

    private Location getBestLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Location gps = null;
        Location network = null;

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (gps == null) {
            return network;
        }
        if (network == null) {
            return gps;
        }

        return gps.getTime() >= network.getTime() ? gps : network;
    }

    private void fetchWeather(Location location) {
        weatherRepository.fetchWeather(location.getLatitude(), location.getLongitude(), isMetric, new WeatherRepository.Callback() {
            @Override
            public void onSuccess(WeatherData data) {
                renderWeather(data, location);
                setLoading(false);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showStatus(message, true);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderWeather(WeatherData data, Location location) {
        String cityText = String.format(Locale.getDefault(),
                "Vị trí hiện tại (%.4f, %.4f)",
                location.getLatitude(),
                location.getLongitude());
        tvLocation.setText(cityText);
        tvCurrentEmoji.setText(WeatherIconUtils.emojiFor(data.getCurrentIconCode(), data.getCurrentCondition()));
        tvCurrentTemp.setText(formatTemperature(data.getCurrentTemp()));
        tvCurrentCondition.setText(data.getCurrentCondition());
        tvCurrentDetails.setText(String.format(Locale.getDefault(),
                "Độ ẩm: %d%%  |  Gió: %s  |  Cảm giác như: %s  |  Cao/Thấp: %s / %s",
                data.getHumidity(),
                formatWindSpeed(data.getWindSpeed()),
                formatTemperature(data.getFeelsLike()),
                formatTemperature(data.getHighTemp()),
                formatTemperature(data.getLowTemp())));

        updateMapLocation(location);

        hourlyAdapter.setItems(data.getHourlyForecast());
        dailyAdapter.setItems(data.getDailyForecast());

        String alertMessage = buildAlertMessage(data);
        data.setAlertMessage(alertMessage);

        if (data.getAlertMessage() != null) {
            cardAlert.setVisibility(View.VISIBLE);
            tvAlert.setText(data.getAlertMessage());
            maybeShowNotification(data.getAlertMessage());
        } else {
            cardAlert.setVisibility(View.GONE);
        }
    }

    private void maybeShowNotification(String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Cảnh báo thời tiết")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(1001, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Cảnh báo thời tiết",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi thời tiết xấu hoặc nhiệt độ vượt ngưỡng");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private String formatTemperature(double value) {
        if (isMetric) {
            return Math.round(value) + "°C";
        }
        return Math.round(value) + "°F";
    }

    private String formatWindSpeed(double value) {
        return String.format(Locale.getDefault(), "%.1f %s", value, isMetric ? "m/s" : "mph");
    }

    private String buildAlertMessage(WeatherData data) {
        String condition = data.getCurrentCondition() == null ? "" : data.getCurrentCondition().toLowerCase(Locale.getDefault());
        double temp = data.getCurrentTemp();
        double wind = data.getWindSpeed();

        double hotThreshold = isMetric ? 38.0 : 100.4;
        double coldThreshold = isMetric ? 0.0 : 32.0;
        double windyThreshold = isMetric ? 12.0 : 26.8;

        if (temp >= hotThreshold) {
            return "Cảnh báo nắng nóng: nhiệt độ hiện tại đang rất cao.";
        }
        if (temp <= coldThreshold) {
            return "Cảnh báo lạnh: nhiệt độ hiện tại đang xuống thấp.";
        }
        if (wind >= windyThreshold) {
            return "Cảnh báo gió mạnh: tốc độ gió đang vượt ngưỡng an toàn.";
        }
        if (condition.contains("dông") || condition.contains("bão") || condition.contains("sấm") || condition.contains("storm")) {
            return "Cảnh báo thời tiết xấu: có khả năng dông bão hoặc mưa lớn.";
        }
        if (condition.contains("mưa lớn") || condition.contains("heavy rain")) {
            return "Cảnh báo mưa lớn: nên hạn chế di chuyển ngoài trời.";
        }
        return null;
    }

    private void setLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);
        btnToggleUnit.setEnabled(!loading);
    }

    private void showStatus(String message, boolean visible) {
        if (!visible || message == null || message.trim().isEmpty()) {
            tvStatus.setVisibility(View.GONE);
            tvStatus.setText("");
            return;
        }
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
    }

    private void updateMapLocation(Location location) {
        if (weatherWebView == null || location == null) {
            return;
        }
        String script = String.format(Locale.US, "moveTo(%.6f,%.6f);", location.getLatitude(), location.getLongitude());
        weatherWebView.evaluateJavascript(script, null);
    }

    private void applyWeatherOverlay() {
        if (weatherWebView == null) {
            return;
        }
        String script = "setWeatherLayer('" + currentMapLayer + "');";
        weatherWebView.evaluateJavascript(script, null);
    }
}