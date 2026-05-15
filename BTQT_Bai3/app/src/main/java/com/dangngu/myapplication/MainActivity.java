package com.dangngu.myapplication;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQ_BLUETOOTH_PERMISSION = 811;
    private static final float LOW_PASS_ALPHA = 0.82f;
    private static final float WAVE_THRESHOLD = 2.35f;
    private static final float TILT_THRESHOLD = 2.2f;
    private static final float ROTATE_THRESHOLD = 2.75f;
    private static final float ROLL_THRESHOLD = 5.8f;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Deque<String> logLines = new ArrayDeque<>();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final List<Animator> radarAnimators = new ArrayList<>();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private AudioManager audioManager;

    private IoTCommandDispatcher commandDispatcher;

    private final float[] gravity = new float[3];
    private final float[] linearAcceleration = new float[3];
    private int waveDirection = 0;
    private int waveSwitches = 0;
    private long waveWindowStart = 0L;
    private long lastWaveTrigger = 0L;
    private long lastWavePeak = 0L;

    private long lastVolumeUpdate = 0L;
    private long lastVolumeCommand = 0L;
    private int volumePercent = 36;
    private int lastVolumeSent = -200;

    private long lastRotateTrigger = 0L;
    private int mediaIndex = 0;

    private long faceDownStart = 0L;
    private long lastNightGuardTrigger = 0L;

    private boolean lightOn = false;
    private boolean fanOn = false;
    private boolean tvOn = false;

    private int currentSection = 0;

    private int maxSystemVolume = 0;
    private boolean ambientAnimationsEnabled = true;

    private final String[] mediaPlaylist = {
            "Neon Grid / Track 01",
            "Digital Drift / Track 02",
            "Cyber Skyline / Track 03",
            "Phantom Signal / Track 04"
    };

    private TextView tvGestureState;
    private TextView tvLightState;
    private TextView tvVolumeState;
    private TextView tvMediaState;
    private TextView tvCreativeState;
    private TextView tvConnectionState;
    private TextView tvCommandLog;

    private TextView tabDashboard;
    private TextView tabIot;
    private TextView tabTimeline;

    private TextView chipChannelWifi;
    private TextView chipChannelBluetooth;

    private View sectionDashboard;
    private View sectionIot;
    private View sectionTimeline;

    private View cardRadar;
    private View cardLight;
    private View cardMedia;
    private View cardCreative;
    private View cardIotGateway;
    private View cardManualControl;
    private View cardTimeline;

    private ProgressBar volumeBar;
    private EditText etWifiEndpoint;
    private EditText etBluetoothAddress;

    private MaterialButton btnLightManual;
    private MaterialButton btnConnectBluetooth;
    private MaterialButton btnDisconnectBluetooth;
    private MaterialButton btnFanToggle;
    private MaterialButton btnTvToggle;
    private MaterialButton btnSendPing;

    private View ringOne;
    private View ringTwo;
    private View ringThree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        initSystemServices();
        initDispatcher();
        initTabs();
        initChannelSelection();
        initButtons();
        initDefaults();
        startEntranceAnimations();
        startRadarPulse();

        appendLog("System online. Gesture engine armed.");
        if (accelerometer == null || gyroscope == null) {
            appendLog("Warning: Sensor hardware is limited on this device.");
            tvGestureState.setText("SENSOR LIMITED");
        }
    }

    private void bindViews() {
        tvGestureState = findViewById(R.id.tvGestureState);
        tvLightState = findViewById(R.id.tvLightState);
        tvVolumeState = findViewById(R.id.tvVolumeState);
        tvMediaState = findViewById(R.id.tvMediaState);
        tvCreativeState = findViewById(R.id.tvCreativeState);
        tvConnectionState = findViewById(R.id.tvConnectionState);
        tvCommandLog = findViewById(R.id.tvCommandLog);

        tabDashboard = findViewById(R.id.tabDashboard);
        tabIot = findViewById(R.id.tabIot);
        tabTimeline = findViewById(R.id.tabTimeline);

        chipChannelWifi = findViewById(R.id.chipChannelWifi);
        chipChannelBluetooth = findViewById(R.id.chipChannelBluetooth);

        sectionDashboard = findViewById(R.id.sectionDashboard);
        sectionIot = findViewById(R.id.sectionIot);
        sectionTimeline = findViewById(R.id.sectionTimeline);

        cardRadar = findViewById(R.id.cardRadar);
        cardLight = findViewById(R.id.cardLight);
        cardMedia = findViewById(R.id.cardMedia);
        cardCreative = findViewById(R.id.cardCreative);
        cardIotGateway = findViewById(R.id.cardIotGateway);
        cardManualControl = findViewById(R.id.cardManualControl);
        cardTimeline = findViewById(R.id.cardTimeline);

        volumeBar = findViewById(R.id.volumeBar);
        etWifiEndpoint = findViewById(R.id.etWifiEndpoint);
        etBluetoothAddress = findViewById(R.id.etBluetoothAddress);

        btnLightManual = findViewById(R.id.btnLightManual);
        btnConnectBluetooth = findViewById(R.id.btnConnectBluetooth);
        btnDisconnectBluetooth = findViewById(R.id.btnDisconnectBluetooth);
        btnFanToggle = findViewById(R.id.btnFanToggle);
        btnTvToggle = findViewById(R.id.btnTvToggle);
        btnSendPing = findViewById(R.id.btnSendPing);

        ringOne = findViewById(R.id.ringOne);
        ringTwo = findViewById(R.id.ringTwo);
        ringThree = findViewById(R.id.ringThree);
    }

    private void initSystemServices() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            maxSystemVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
    }

    private void initDispatcher() {
        commandDispatcher = new IoTCommandDispatcher(this);
        commandDispatcher.setChannel(IoTCommandDispatcher.Channel.WIFI);
    }

    private void initTabs() {
        tabDashboard.setOnClickListener(v -> showSection(0));
        tabIot.setOnClickListener(v -> showSection(1));
        tabTimeline.setOnClickListener(v -> showSection(2));
        updateTabSelection();
    }

    private void initChannelSelection() {
        chipChannelWifi.setOnClickListener(v -> setChannel(IoTCommandDispatcher.Channel.WIFI));
        chipChannelBluetooth.setOnClickListener(v -> setChannel(IoTCommandDispatcher.Channel.BLUETOOTH));
        updateChannelChips();
    }

    private void initButtons() {
        btnLightManual.setOnClickListener(v -> toggleLight("manual"));
        btnFanToggle.setOnClickListener(v -> toggleFan());
        btnTvToggle.setOnClickListener(v -> toggleTv());
        btnSendPing.setOnClickListener(v -> {
            animateTap(v);
            dispatchCommand("PING", "manual check");
        });

        btnConnectBluetooth.setOnClickListener(v -> {
            animateTap(v);
            requestBluetoothAndConnect();
        });
        btnDisconnectBluetooth.setOnClickListener(v -> {
            animateTap(v);
            disconnectBluetooth();
        });
    }

    private void initDefaults() {
        volumeBar.setMax(100);
        volumeBar.setProgress(volumePercent);
        etWifiEndpoint.setText("http://192.168.1.100:8080/command");

        updateLightUI("idle");
        updateMediaUI("idle");
        updateVolumeUI("idle");
        updateCreativeUI("Night Guard: waiting");
        updateFanButton();
        updateTvButton();
        updateConnectionUI();
        tvGestureState.setText("LISTENING...");
    }

    private void startEntranceAnimations() {
        View[] cards = {
                cardRadar, cardLight, cardMedia, cardCreative, cardIotGateway, cardManualControl, cardTimeline
        };
        long delay = 120L;
        for (View card : cards) {
            card.setAlpha(0f);
            card.setTranslationY(64f);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(1200L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            delay += 170L;
        }

        ObjectAnimator titlePulse = ObjectAnimator.ofFloat(findViewById(R.id.tvTopTitle), View.ALPHA, 0.56f, 1f);
        titlePulse.setDuration(1600L);
        titlePulse.setRepeatMode(ObjectAnimator.REVERSE);
        titlePulse.setRepeatCount(ObjectAnimator.INFINITE);
        radarAnimators.add(titlePulse);
        titlePulse.start();
    }

    private void startRadarPulse() {
        ObjectAnimator ring1 = buildRingAnimator(ringOne, 0);
        ObjectAnimator ring2 = buildRingAnimator(ringTwo, 600);
        ObjectAnimator ring3 = buildRingAnimator(ringThree, 1200);
        radarAnimators.add(ring1);
        radarAnimators.add(ring2);
        radarAnimators.add(ring3);
        ring1.start();
        ring2.start();
        ring3.start();
    }

    private ObjectAnimator buildRingAnimator(View ringView, long startDelay) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(ringView, View.SCALE_X, 0.35f, 1.6f);
        pulse.setDuration(2800L);
        pulse.setStartDelay(startDelay);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(animation -> {
            float progress = animation.getAnimatedFraction();
            ringView.setScaleY(0.35f + (1.25f * progress));
            ringView.setAlpha(Math.max(0f, 0.8f - (progress * 0.8f)));
        });
        return pulse;
    }

    private void showSection(int targetSection) {
        if (targetSection == currentSection) {
            return;
        }

        View current = sectionFor(currentSection);
        View target = sectionFor(targetSection);
        if (current == null || target == null) {
            return;
        }

        int direction = targetSection > currentSection ? 1 : -1;
        target.setVisibility(View.VISIBLE);
        target.setAlpha(0f);
        target.setTranslationX(direction * 160f);
        target.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(1200L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        current.animate()
                .alpha(0f)
                .translationX(direction * -160f)
                .setDuration(980L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    current.setVisibility(View.GONE);
                    current.setAlpha(1f);
                    current.setTranslationX(0f);
                })
                .start();

        currentSection = targetSection;
        updateTabSelection();
    }

    private View sectionFor(int index) {
        if (index == 0) {
            return sectionDashboard;
        }
        if (index == 1) {
            return sectionIot;
        }
        if (index == 2) {
            return sectionTimeline;
        }
        return null;
    }

    private void updateTabSelection() {
        styleTab(tabDashboard, currentSection == 0);
        styleTab(tabIot, currentSection == 1);
        styleTab(tabTimeline, currentSection == 2);
    }

    private void styleTab(TextView tab, boolean active) {
        tab.setBackgroundResource(active ? R.drawable.tab_active : R.drawable.tab_idle);
        tab.setTextColor(ContextCompat.getColor(this, active ? R.color.hacker_black : R.color.matrix_green_soft));
    }

    private void setChannel(IoTCommandDispatcher.Channel channel) {
        commandDispatcher.setChannel(channel);
        updateChannelChips();
        updateConnectionUI();
        appendLog("Channel switched to " + channel.name());
    }

    private void updateChannelChips() {
        boolean wifiActive = commandDispatcher.getChannel() == IoTCommandDispatcher.Channel.WIFI;
        chipChannelWifi.setBackgroundResource(wifiActive ? R.drawable.tab_active : R.drawable.tab_idle);
        chipChannelWifi.setTextColor(ContextCompat.getColor(this, wifiActive ? R.color.hacker_black : R.color.matrix_green_soft));
        chipChannelBluetooth.setBackgroundResource(!wifiActive ? R.drawable.tab_active : R.drawable.tab_idle);
        chipChannelBluetooth.setTextColor(ContextCompat.getColor(this, !wifiActive ? R.color.hacker_black : R.color.matrix_green_soft));
    }

    private void requestBluetoothAndConnect() {
        if (!commandDispatcher.hasBluetoothSupport()) {
            appendLog("Bluetooth is not available on this phone.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQ_BLUETOOTH_PERMISSION
            );
            return;
        }
        connectBluetoothNow();
    }

    @SuppressLint("MissingPermission")
    private void connectBluetoothNow() {
        if (!commandDispatcher.isBluetoothEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            appendLog("Please enable Bluetooth, then tap connect again.");
            return;
        }
        commandDispatcher.setBluetoothAddress(etBluetoothAddress.getText().toString());
        commandDispatcher.connectBluetooth((success, message) -> {
            appendLog(message);
            updateConnectionUI();
        });
    }

    @SuppressLint("MissingPermission")
    private void disconnectBluetooth() {
        commandDispatcher.disconnectBluetooth((success, message) -> {
            appendLog(message);
            updateConnectionUI();
        });
    }

    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectBluetoothNow();
            } else {
                appendLog("Bluetooth permission denied.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            }
        }
        ambientAnimationsEnabled = true;
    }

    @Override
    protected void onPause() {
        ambientAnimationsEnabled = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        for (Animator animator : radarAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        uiHandler.removeCallbacksAndMessages(null);
        if (commandDispatcher != null) {
            commandDispatcher.release();
        }
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = (LOW_PASS_ALPHA * gravity[0]) + ((1f - LOW_PASS_ALPHA) * event.values[0]);
            gravity[1] = (LOW_PASS_ALPHA * gravity[1]) + ((1f - LOW_PASS_ALPHA) * event.values[1]);
            gravity[2] = (LOW_PASS_ALPHA * gravity[2]) + ((1f - LOW_PASS_ALPHA) * event.values[2]);

            linearAcceleration[0] = event.values[0] - gravity[0];
            linearAcceleration[1] = event.values[1] - gravity[1];
            linearAcceleration[2] = event.values[2] - gravity[2];

            detectWave(linearAcceleration[0]);
            detectTiltForVolume(gravity[1]);
            detectFaceDownMacro(gravity[2]);
            detectRollForMedia(gravity[0]);
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            detectRotationForMedia(event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void detectWave(float xAxis) {
        if (Math.abs(xAxis) < WAVE_THRESHOLD) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - lastWavePeak) < 130L) {
            return;
        }
        lastWavePeak = now;
        int direction = xAxis > 0f ? 1 : -1;

        if (waveWindowStart == 0L || (now - waveWindowStart) > 1900L) {
            waveWindowStart = now;
            waveSwitches = 0;
            waveDirection = direction;
            return;
        }

        if (direction != waveDirection) {
            waveDirection = direction;
            waveSwitches++;
            pulseGestureLabel();
            if (waveSwitches >= 2 && (now - lastWaveTrigger) > 1200L) {
                lastWaveTrigger = now;
                waveSwitches = 0;
                waveWindowStart = 0L;
                tvGestureState.setText("WAVE -> TOGGLE LIGHT");
                toggleLight("gesture wave");
            }
        }
    }

    private void detectTiltForVolume(float yAxis) {
        long now = System.currentTimeMillis();
        if (Math.abs(yAxis) < TILT_THRESHOLD || (now - lastVolumeUpdate) < 170L) {
            return;
        }
        lastVolumeUpdate = now;

        int delta = Math.round((-yAxis / 9.81f) * 4f);
        if (delta == 0) {
            return;
        }

        int newVolume = clamp(volumePercent + delta, 0, 100);
        if (newVolume == volumePercent) {
            return;
        }

        volumePercent = newVolume;
        updateVolumeUI("tilt");
        tvGestureState.setText("TILT -> VOLUME " + volumePercent + "%");
        pulseGestureLabel();

        if (Math.abs(volumePercent - lastVolumeSent) >= 6 || (now - lastVolumeCommand) > 1000L) {
            lastVolumeSent = volumePercent;
            lastVolumeCommand = now;
            dispatchCommand("VOLUME_" + volumePercent, "gesture tilt");
        }
    }

    private void detectRotationForMedia(float zAxis) {
        long now = System.currentTimeMillis();
        if (Math.abs(zAxis) < ROTATE_THRESHOLD || (now - lastRotateTrigger) < 1300L) {
            return;
        }
        lastRotateTrigger = now;

        boolean next = zAxis > 0;
        mediaIndex = next
                ? (mediaIndex + 1) % mediaPlaylist.length
                : (mediaIndex - 1 + mediaPlaylist.length) % mediaPlaylist.length;

        updateMediaUI(next ? "rotate right" : "rotate left");
        tvGestureState.setText(next ? "ROTATE RIGHT -> NEXT" : "ROTATE LEFT -> PREV");
        pulseGestureLabel();
        dispatchCommand(next ? "MEDIA_NEXT" : "MEDIA_PREV", "gesture rotate");
    }

    private void detectRollForMedia(float xGravity) {
        long now = System.currentTimeMillis();
        if (Math.abs(xGravity) < ROLL_THRESHOLD || (now - lastRotateTrigger) < 1300L) {
            return;
        }
        lastRotateTrigger = now;

        boolean next = xGravity > 0;
        mediaIndex = next
                ? (mediaIndex + 1) % mediaPlaylist.length
                : (mediaIndex - 1 + mediaPlaylist.length) % mediaPlaylist.length;

        updateMediaUI(next ? "roll right" : "roll left");
        tvGestureState.setText(next ? "ROLL RIGHT -> NEXT" : "ROLL LEFT -> PREV");
        pulseGestureLabel();
        dispatchCommand(next ? "MEDIA_NEXT" : "MEDIA_PREV", "gesture roll");
    }

    private void detectFaceDownMacro(float zAxis) {
        long now = System.currentTimeMillis();
        if (zAxis < -8.4f) {
            if (faceDownStart == 0L) {
                faceDownStart = now;
            }
            if ((now - faceDownStart) > 1500L && (now - lastNightGuardTrigger) > 8000L) {
                faceDownStart = 0L;
                lastNightGuardTrigger = now;
                triggerNightGuard();
            }
        } else {
            faceDownStart = 0L;
        }
    }

    private void triggerNightGuard() {
        lightOn = false;
        fanOn = false;
        tvOn = false;
        volumePercent = 12;

        updateLightUI("Night Guard");
        updateFanButton();
        updateTvButton();
        updateVolumeUI("Night Guard");
        updateCreativeUI("Night Guard active: all devices OFF");

        tvGestureState.setText("FACE DOWN -> NIGHT GUARD");
        pulseGestureLabel();
        dispatchCommand("SCENE_NIGHT_GUARD", "creative macro");
    }

    private void toggleLight(String source) {
        lightOn = !lightOn;
        updateLightUI(source);
        dispatchCommand(lightOn ? "LIGHT_ON" : "LIGHT_OFF", source);
    }

    private void toggleFan() {
        fanOn = !fanOn;
        updateFanButton();
        dispatchCommand(fanOn ? "FAN_ON" : "FAN_OFF", "manual fan");
    }

    private void toggleTv() {
        tvOn = !tvOn;
        updateTvButton();
        dispatchCommand(tvOn ? "TV_ON" : "TV_OFF", "manual tv");
    }

    private void updateLightUI(String source) {
        String value = lightOn ? "ON" : "OFF";
        tvLightState.setText("Light: " + value + "  [" + source + "]");
        tvLightState.setTextColor(ContextCompat.getColor(this, lightOn ? R.color.matrix_green : R.color.matrix_green_dim));
        btnLightManual.setText(lightOn ? "FORCE LIGHT OFF" : "FORCE LIGHT ON");
    }

    private void updateVolumeUI(String source) {
        volumeBar.setProgress(volumePercent);
        tvVolumeState.setText("Volume: " + volumePercent + "%  [" + source + "]");
        if (audioManager != null && maxSystemVolume > 0) {
            int streamVolume = clamp(Math.round((volumePercent / 100f) * maxSystemVolume), 0, maxSystemVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolume, 0);
        }
    }

    private void updateMediaUI(String source) {
        tvMediaState.setText("Now: " + mediaPlaylist[mediaIndex] + "  [" + source + "]");
    }

    private void updateCreativeUI(String state) {
        tvCreativeState.setText(state);
    }

    private void updateFanButton() {
        btnFanToggle.setText(fanOn ? "FAN: ON" : "FAN: OFF");
    }

    private void updateTvButton() {
        btnTvToggle.setText(tvOn ? "TV: ON" : "TV: OFF");
    }

    private void updateConnectionUI() {
        boolean wifi = commandDispatcher.getChannel() == IoTCommandDispatcher.Channel.WIFI;
        if (wifi) {
            tvConnectionState.setText("Channel: WiFi endpoint active");
            tvConnectionState.setTextColor(ContextCompat.getColor(this, R.color.matrix_green));
        } else if (commandDispatcher.isBluetoothConnected()) {
            tvConnectionState.setText("Channel: Bluetooth connected");
            tvConnectionState.setTextColor(ContextCompat.getColor(this, R.color.matrix_green));
        } else {
            tvConnectionState.setText("Channel: Bluetooth not connected");
            tvConnectionState.setTextColor(ContextCompat.getColor(this, R.color.matrix_green_dim));
        }
    }

    private void dispatchCommand(String command, String source) {
        commandDispatcher.setWifiEndpoint(etWifiEndpoint.getText().toString());
        commandDispatcher.setBluetoothAddress(etBluetoothAddress.getText().toString());
        commandDispatcher.dispatchCommand(command, (success, message) -> {
            String status = success ? "OK" : "ERR";
            appendLog(status + " " + source + " -> " + command + " | " + message);
            if (ambientAnimationsEnabled) {
                pulseGestureLabel();
            }
            updateConnectionUI();
        });
    }

    private void pulseGestureLabel() {
        tvGestureState.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(150L)
                .withEndAction(() -> tvGestureState.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(360L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
    }

    private void animateTap(View view) {
        view.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(70L)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(160L).start())
                .start();
    }

    private void appendLog(String line) {
        String stampedLine = "[" + clock.format(new Date()) + "] " + line;
        logLines.addFirst(stampedLine);
        while (logLines.size() > 24) {
            logLines.removeLast();
        }

        StringBuilder builder = new StringBuilder();
        for (String item : logLines) {
            builder.append(item).append('\n');
        }
        tvCommandLog.setText(builder.toString());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
