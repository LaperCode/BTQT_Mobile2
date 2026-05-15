package com.example.ex_2_the_weather_app;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "weather_prefs";
    private static final String KEY_IS_METRIC = "is_metric";
    private static final String CHANNEL_ID = "weather_alerts";
    private static final String OPEN_WEATHER_API_KEY = "cd85c9c01e5bf1e8a77d1b156ce598e9";
    private static final String KEY_HOT_THRESHOLD_C = "hot_threshold_c";
    private static final String KEY_HOT_THRESHOLD_F = "hot_threshold_f";
    private static final String KEY_COLD_THRESHOLD_C = "cold_threshold_c";
    private static final String KEY_COLD_THRESHOLD_F = "cold_threshold_f";
    private static final String KEY_WIND_THRESHOLD_MS = "wind_threshold_ms";
    private static final String KEY_WIND_THRESHOLD_MPH = "wind_threshold_mph";
    private static final String ALERT_TTS_ID = "weather_alert";
    private static final long SPEECH_REPEAT_DELAY_MS = 700;

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
    private Button btnDisasterStorm;
    private Button btnDisasterFlood;
    private Button btnDisasterEarthquake;
    private Button btnDisasterThreshold;
    private Button btnStopAlert;
    private WebView weatherWebView;
    private WeatherForecastAdapter hourlyAdapter;
    private WeatherForecastAdapter dailyAdapter;
    private WeatherRepository weatherRepository;
    private AddressRepository addressRepository;
    private SharedPreferences preferences;
    private boolean isMetric;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private String currentMapLayer = "clouds_new";
    private ValueAnimator alertPulseAnimator;
    private TextToSpeech textToSpeech;
    private boolean ttsReady;
    private boolean alertActive;
    private String activeAlertMessage;
    private Vibrator alertVibrator;
    private final Handler alertHandler = new Handler(Looper.getMainLooper());
    private final Runnable speakRunnable = new Runnable() {
        @Override
        public void run() {
            speakActiveAlert();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isMetric = preferences.getBoolean(KEY_IS_METRIC, true);
        weatherRepository = new WeatherRepository();
        addressRepository = new AddressRepository(this);
        initTextToSpeech();

        bindViews();
        setupLists();
        setupPermissionLauncher();
        setupActions();
        updateUnitButton();
        initMap();
        requestWeatherPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlertContinuousEffects();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        ttsReady = false;
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
        btnDisasterStorm = findViewById(R.id.btnDisasterStorm);
        btnDisasterFlood = findViewById(R.id.btnDisasterFlood);
        btnDisasterEarthquake = findViewById(R.id.btnDisasterEarthquake);
        btnDisasterThreshold = findViewById(R.id.btnDisasterThreshold);
        btnStopAlert = findViewById(R.id.btnStopAlert);
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

        btnDisasterStorm.setOnClickListener(v -> showDisasterAlert(
            "Cảnh báo bão: hãy gia cố nhà cửa và hạn chế ra ngoài khi không cần thiết."
        ));

        btnDisasterFlood.setOnClickListener(v -> showDisasterAlert(
            "Cảnh báo lũ lụt: di chuyển lên khu vực cao và tránh các tuyến đường ngập sâu."
        ));

        btnDisasterEarthquake.setOnClickListener(v -> showDisasterAlert(
            "Cảnh báo động đất: giữ bình tĩnh, tránh xa cửa kính và tìm nơi trú ẩn an toàn."
        ));

        btnDisasterThreshold.setOnClickListener(v -> showDisasterThresholdDialog());

        btnStopAlert.setOnClickListener(v -> stopAlertNow());
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
                reverseGeocodeAndRender(data, location);
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

    private void reverseGeocodeAndRender(WeatherData data, Location location) {
        addressRepository.getAddress(location.getLatitude(), location.getLongitude(), new AddressRepository.AddressCallback() {
            @Override
            public void onSuccess(String address) {
                renderWeather(data, location, address);
            }

            @Override
            public void onError(String message) {
                String fallback = String.format(Locale.getDefault(), "%.4f, %.4f", location.getLatitude(), location.getLongitude());
                renderWeather(data, location, fallback);
            }
        });
    }

    private void renderWeather(WeatherData data, Location location, String address) {
        String cityText = address != null && !address.isEmpty() ? address : String.format(Locale.getDefault(),
                "%.4f, %.4f", location.getLatitude(), location.getLongitude());
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
            startAlertPulse();
            startAlertContinuousEffects(data.getAlertMessage(), false);
            maybeShowNotification(data.getAlertMessage());
            btnStopAlert.setVisibility(View.VISIBLE);
        } else {
            cardAlert.setVisibility(View.GONE);
            stopAlertPulse();
            stopAlertContinuousEffects();
            btnStopAlert.setVisibility(View.GONE);
        }
    }

    private void maybeShowNotification(String message) {
        showNotification("Cảnh báo thời tiết", message);
    }

    private void showDisasterAlert(String message) {
        cardAlert.setVisibility(View.VISIBLE);
        tvAlert.setText(message);
        startAlertPulse();
        startAlertContinuousEffects(message, true);
        showNotification("Cảnh báo thảm họa", message);
        btnStopAlert.setVisibility(View.VISIBLE);
    }

    private void showDisasterThresholdDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_disaster_threshold, null);
        TextInputEditText inputHotC = dialogView.findViewById(R.id.inputHotC);
        TextInputEditText inputHotF = dialogView.findViewById(R.id.inputHotF);
        TextInputEditText inputColdC = dialogView.findViewById(R.id.inputColdC);
        TextInputEditText inputColdF = dialogView.findViewById(R.id.inputColdF);
        TextInputEditText inputWindMs = dialogView.findViewById(R.id.inputWindMs);
        TextInputEditText inputWindMph = dialogView.findViewById(R.id.inputWindMph);

        populateThresholdPair(inputHotC, inputHotF, KEY_HOT_THRESHOLD_C, KEY_HOT_THRESHOLD_F,
                this::celsiusToFahrenheit, this::fahrenheitToCelsius);
        populateThresholdPair(inputColdC, inputColdF, KEY_COLD_THRESHOLD_C, KEY_COLD_THRESHOLD_F,
                this::celsiusToFahrenheit, this::fahrenheitToCelsius);
        populateThresholdPair(inputWindMs, inputWindMph, KEY_WIND_THRESHOLD_MS, KEY_WIND_THRESHOLD_MPH,
                this::metersPerSecondToMph, this::mphToMetersPerSecond);

        bindTwoWayConversion(inputHotC, inputHotF, this::celsiusToFahrenheit, this::fahrenheitToCelsius);
        bindTwoWayConversion(inputColdC, inputColdF, this::celsiusToFahrenheit, this::fahrenheitToCelsius);
        bindTwoWayConversion(inputWindMs, inputWindMph, this::metersPerSecondToMph, this::mphToMetersPerSecond);

        new AlertDialog.Builder(this)
                .setTitle("Ngưỡng thảm họa")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String hotCText = getInputText(inputHotC);
                    String hotFText = getInputText(inputHotF);
                    String coldCText = getInputText(inputColdC);
                    String coldFText = getInputText(inputColdF);
                    String windMsText = getInputText(inputWindMs);
                    String windMphText = getInputText(inputWindMph);
                    preferences.edit()
                            .putString(KEY_HOT_THRESHOLD_C, hotCText)
                            .putString(KEY_HOT_THRESHOLD_F, hotFText)
                            .putString(KEY_COLD_THRESHOLD_C, coldCText)
                            .putString(KEY_COLD_THRESHOLD_F, coldFText)
                            .putString(KEY_WIND_THRESHOLD_MS, windMsText)
                            .putString(KEY_WIND_THRESHOLD_MPH, windMphText)
                            .apply();
                    Toast.makeText(this, "Đã lưu ngưỡng thảm họa.", Toast.LENGTH_SHORT).show();
                    requestWeatherPermissions();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(1001, builder.build());
    }

    private void startAlertPulse() {
        if (cardAlert == null) {
            return;
        }
        stopAlertPulse();
        alertPulseAnimator = ValueAnimator.ofArgb(
                0xFFFFD54F,
                0xFFFF8A65,
                0xFFEF5350,
                0xFFFFD54F
        );
        alertPulseAnimator.setDuration(350);
        alertPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alertPulseAnimator.setRepeatMode(ValueAnimator.RESTART);
        alertPulseAnimator.addUpdateListener(animation ->
                cardAlert.setCardBackgroundColor((Integer) animation.getAnimatedValue())
        );
        alertPulseAnimator.start();
    }

    private void stopAlertPulse() {
        if (alertPulseAnimator != null) {
            alertPulseAnimator.cancel();
            alertPulseAnimator = null;
        }
        if (cardAlert != null) {
            cardAlert.setCardBackgroundColor(0xFFFFF4E5);
        }
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) {
                ttsReady = false;
                return;
            }
            int result = textToSpeech.setLanguage(new Locale("vi", "VN"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                    if (!ALERT_TTS_ID.equals(utteranceId)) {
                        return;
                    }
                    if (!alertActive) {
                        return;
                    }
                    alertHandler.postDelayed(speakRunnable, SPEECH_REPEAT_DELAY_MS);
                }

                @Override
                public void onError(String utteranceId) {
                    if (!ALERT_TTS_ID.equals(utteranceId)) {
                        return;
                    }
                    if (!alertActive) {
                        return;
                    }
                    alertHandler.postDelayed(speakRunnable, SPEECH_REPEAT_DELAY_MS);
                }
            });
            ttsReady = true;
            if (alertActive && activeAlertMessage != null) {
                speakActiveAlert();
            }
        });
    }

    private void startAlertContinuousEffects(String message, boolean force) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        if (!force && alertActive && message.equals(activeAlertMessage)) {
            return;
        }
        alertActive = true;
        activeAlertMessage = message;
        startVibrationLoop();
        speakActiveAlert();
    }

    private void stopAlertContinuousEffects() {
        alertActive = false;
        activeAlertMessage = null;
        stopVibrationLoop();
        stopSpeakingLoop();
    }

    private void stopAlertNow() {
        stopAlertPulse();
        stopAlertContinuousEffects();
        cardAlert.setVisibility(View.GONE);
        tvAlert.setText("");
        btnStopAlert.setVisibility(View.GONE);
    }

    private void speakActiveAlert() {
        if (!ttsReady || textToSpeech == null || !alertActive || activeAlertMessage == null) {
            return;
        }
        textToSpeech.speak(activeAlertMessage, TextToSpeech.QUEUE_FLUSH, null, ALERT_TTS_ID);
    }

    private void stopSpeakingLoop() {
        alertHandler.removeCallbacks(speakRunnable);
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    private void startVibrationLoop() {
        Vibrator vibrator = getVibrator();
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        long[] pattern = new long[]{0, 120, 80, 120, 80, 240};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, 0);
        }
        alertVibrator = vibrator;
    }

    private void stopVibrationLoop() {
        if (alertVibrator != null) {
            alertVibrator.cancel();
            alertVibrator = null;
        }
    }

    private Vibrator getVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            if (manager != null) {
                return manager.getDefaultVibrator();
            }
        }
        return (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Cảnh báo thời tiết",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi thời tiết xấu hoặc thảm họa nguy hiểm");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private String getInputText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private Double parseNumber(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatNumber(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private double getThresholdValue(String key, double fallback) {
        Double value = parseNumber(preferences.getString(key, ""));
        return value != null ? value : fallback;
    }

    private double celsiusToFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    private double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32.0) * 5.0 / 9.0;
    }

    private double metersPerSecondToMph(double value) {
        return value * 2.2369362920544;
    }

    private double mphToMetersPerSecond(double value) {
        return value / 2.2369362920544;
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

        double hotThreshold = isMetric
            ? getThresholdValue(KEY_HOT_THRESHOLD_C, 38.0)
            : getThresholdValue(KEY_HOT_THRESHOLD_F, 100.4);
        double coldThreshold = isMetric
            ? getThresholdValue(KEY_COLD_THRESHOLD_C, 0.0)
            : getThresholdValue(KEY_COLD_THRESHOLD_F, 32.0);
        double windyThreshold = isMetric
            ? getThresholdValue(KEY_WIND_THRESHOLD_MS, 12.0)
            : getThresholdValue(KEY_WIND_THRESHOLD_MPH, 26.8);

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

    private interface Converter {
        double convert(double value);
    }

    private void populateThresholdPair(TextInputEditText primary, TextInputEditText secondary,
                                       String primaryKey, String secondaryKey,
                                       Converter primaryToSecondary, Converter secondaryToPrimary) {
        String primaryValue = preferences.getString(primaryKey, "");
        String secondaryValue = preferences.getString(secondaryKey, "");

        if (primaryValue != null && !primaryValue.isEmpty()) {
            primary.setText(primaryValue);
        }
        if (secondaryValue != null && !secondaryValue.isEmpty()) {
            secondary.setText(secondaryValue);
        }

        if (primaryValue != null && !primaryValue.isEmpty()
                && (secondaryValue == null || secondaryValue.isEmpty())) {
            Double parsed = parseNumber(primaryValue);
            if (parsed != null) {
                secondary.setText(formatNumber(primaryToSecondary.convert(parsed)));
            }
        } else if ((primaryValue == null || primaryValue.isEmpty())
                && secondaryValue != null && !secondaryValue.isEmpty()) {
            Double parsed = parseNumber(secondaryValue);
            if (parsed != null) {
                primary.setText(formatNumber(secondaryToPrimary.convert(parsed)));
            }
        }
    }

    private void bindTwoWayConversion(TextInputEditText primary, TextInputEditText secondary,
                                      Converter primaryToSecondary, Converter secondaryToPrimary) {
        boolean[] isUpdating = {false};

        primary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating[0]) {
                    return;
                }
                isUpdating[0] = true;
                Double value = parseNumber(getInputText(primary));
                if (value == null) {
                    secondary.setText("");
                } else {
                    secondary.setText(formatNumber(primaryToSecondary.convert(value)));
                }
                isUpdating[0] = false;
            }
        });

        secondary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating[0]) {
                    return;
                }
                isUpdating[0] = true;
                Double value = parseNumber(getInputText(secondary));
                if (value == null) {
                    primary.setText("");
                } else {
                    primary.setText(formatNumber(secondaryToPrimary.convert(value)));
                }
                isUpdating[0] = false;
            }
        });
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