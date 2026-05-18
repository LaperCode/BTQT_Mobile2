package com.dangngu.myapplication;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    private static final int REQ_DEVICE_ADMIN = 812;
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
    private Sensor proximitySensor;
    private AudioManager audioManager;
    private CameraManager cameraManager;
    private String cameraId;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

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

    private boolean flashOn = false;
    private boolean fanOn = false;

    // Proximity wave detection
    private boolean proxNear = false;
    private long lastProxToggle = 0L;
    private boolean tvOn = false;

    private int currentSection = 0;

    private int maxSystemVolume = 0;
    private boolean ambientAnimationsEnabled = true;

    // Media player
    private MediaPlayer mediaPlayer;
    private boolean mediaPlaying = false;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private final String[] mediaPlaylist = {
            "Neon Grid / Track 01",
            "Digital Drift / Track 02",
            "Cyber Skyline / Track 03",
            "Phantom Signal / Track 04"
    };

    // Map tracks to raw resource IDs (will be set in initMediaPlayer)
    private int[] mediaResIds;

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
    private ProgressBar mediaProgressBar;
    private EditText etWifiEndpoint;
    private EditText etBluetoothAddress;

    private MaterialButton btnLightManual;
    private MaterialButton btnConnectBluetooth;
    private MaterialButton btnDisconnectBluetooth;
    private MaterialButton btnFanToggle;
    private MaterialButton btnTvToggle;
    private MaterialButton btnSendPing;
    private MaterialButton btnMediaPrev;
    private MaterialButton btnMediaPlayPause;
    private MaterialButton btnMediaNext;

    private View ringOne;
    private View ringTwo;
    private View ringThree;

    // Flashlight visual
    private TextView tvFlashIcon;
    private TextView tvFlashOnOff;
    private FrameLayout flashContainer;
    private TextView tvProximityState;

    // Media track display
    private TextView tvMediaTrackName;
    private TextView tvMediaPlayStatus;

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
        initMediaPlayer();
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
        mediaProgressBar = findViewById(R.id.mediaProgressBar);
        etWifiEndpoint = findViewById(R.id.etWifiEndpoint);
        etBluetoothAddress = findViewById(R.id.etBluetoothAddress);

        btnLightManual = findViewById(R.id.btnLightManual);
        btnConnectBluetooth = findViewById(R.id.btnConnectBluetooth);
        btnDisconnectBluetooth = findViewById(R.id.btnDisconnectBluetooth);
        btnFanToggle = findViewById(R.id.btnFanToggle);
        btnTvToggle = findViewById(R.id.btnTvToggle);
        btnSendPing = findViewById(R.id.btnSendPing);
        btnMediaPrev = findViewById(R.id.btnMediaPrev);
        btnMediaPlayPause = findViewById(R.id.btnMediaPlayPause);
        btnMediaNext = findViewById(R.id.btnMediaNext);

        ringOne = findViewById(R.id.ringOne);
        ringTwo = findViewById(R.id.ringTwo);
        ringThree = findViewById(R.id.ringThree);

        // Flashlight visual
        tvFlashIcon = findViewById(R.id.tvFlashIcon);
        tvFlashOnOff = findViewById(R.id.tvFlashOnOff);
        flashContainer = findViewById(R.id.flashContainer);
        tvProximityState = findViewById(R.id.tvProximityState);

        // Media track display
        tvMediaTrackName = findViewById(R.id.tvMediaTrackName);
        tvMediaPlayStatus = findViewById(R.id.tvMediaPlayStatus);
    }

    private void initSystemServices() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            maxSystemVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        // Init camera manager for flashlight
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                String[] ids = cameraManager.getCameraIdList();
                if (ids.length > 0) {
                    cameraId = ids[0];
                }
            }
        } catch (CameraAccessException e) {
            appendLog("Camera init error: " + e.getMessage());
        }

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, ScreenLockAdminReceiver.class);
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
        btnLightManual.setOnClickListener(v -> toggleFlash("manual"));
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

        // Media controls
        btnMediaPlayPause.setOnClickListener(v -> {
            animateTap(v);
            toggleMediaPlayback();
        });
        btnMediaPrev.setOnClickListener(v -> {
            animateTap(v);
            switchTrack(false);
        });
        btnMediaNext.setOnClickListener(v -> {
            animateTap(v);
            switchTrack(true);
        });
    }

    private void initDefaults() {
        volumeBar.setMax(100);
        volumeBar.setProgress(volumePercent);
        mediaProgressBar.setMax(100);
        mediaProgressBar.setProgress(0);
        etWifiEndpoint.setText("http://192.168.1.100:8080/command");

        updateFlashUI("idle");
        updateMediaUI("idle");
        updateVolumeUI("idle");
        updateCreativeUI("Night Guard: waiting");
        updateFanButton();
        updateTvButton();
        updateConnectionUI();
        updateMediaPlaybackUI();
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
            if (proximitySensor != null) {
                sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
        ambientAnimationsEnabled = true;
        // Resume media if it was playing
        if (mediaPlayer != null && mediaPlaying && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        ambientAnimationsEnabled = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Pause media when app goes to background
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
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
        progressHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (commandDispatcher != null) {
            commandDispatcher.release();
        }
        // Turn off flashlight when app is destroyed
        if (flashOn) {
            setFlashlight(false);
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

            detectTiltForVolume(gravity[1]);
            detectFaceDownMacro(gravity[2]);
            detectRollForMedia(gravity[0]);
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            detectRotationForMedia(event.values[2]);
        }

        // Proximity sensor: detect hand wave in front of screen
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            detectProximityWave(event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Detect hand wave using proximity sensor.
     * When hand comes near (value < max) then moves away (value >= max),
     * that counts as one wave => toggle flashlight.
     */
    private void detectProximityWave(float distance) {
        float maxRange = proximitySensor != null ? proximitySensor.getMaximumRange() : 5f;
        boolean isNear = distance < maxRange;
        long now = System.currentTimeMillis();

        if (isNear) {
            // Hand is near the screen
            if (!proxNear) {
                proxNear = true;
                tvProximityState.setText("Proximity: NEAR (hand detected)");
                tvProximityState.setTextColor(ContextCompat.getColor(this, R.color.matrix_green));
                pulseGestureLabel();
            }
        } else {
            // Hand moved away - if it was near before, this is a complete wave
            if (proxNear) {
                proxNear = false;
                tvProximityState.setText("Proximity: FAR (wave complete)");
                tvProximityState.setTextColor(ContextCompat.getColor(this, R.color.media_cyan));

                // Debounce: at least 800ms between toggles
                if ((now - lastProxToggle) > 800L) {
                    lastProxToggle = now;
                    tvGestureState.setText("WAVE -> TOGGLE FLASH");
                    toggleFlash("proximity wave");
                    pulseGestureLabel();
                }
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
        switchTrack(next);
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
        switchTrack(next);
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
                lockScreenIfAllowed();
            }
        } else {
            faceDownStart = 0L;
        }
    }

    private void lockScreenIfAllowed() {
        if (devicePolicyManager == null || adminComponent == null) {
            appendLog("Device admin not available on this device.");
            return;
        }

        if (devicePolicyManager.isAdminActive(adminComponent)) {
            // Admin is active – lock the screen immediately
            devicePolicyManager.lockNow();
            tvGestureState.setText("FACE DOWN -> SCREEN OFF");
            pulseGestureLabel();
            appendLog("Screen locked via Device Admin.");
        } else {
            // Request device admin permission every time until granted
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enable device admin to allow face-down screen lock.");
            startActivityForResult(intent, REQ_DEVICE_ADMIN);
            appendLog("Requesting Device Admin permission for screen lock.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_DEVICE_ADMIN) {
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent)) {
                appendLog("Device Admin enabled! Face-down will now lock screen.");
                // Lock immediately since the face-down gesture was already detected
                devicePolicyManager.lockNow();
            } else {
                appendLog("Device Admin was not enabled. Screen lock won't work.");
            }
        }
    }

    private void triggerNightGuard() {
        // Turn off flash if it's on
        if (flashOn) {
            flashOn = false;
            setFlashlight(false);
        }
        fanOn = false;
        tvOn = false;
        volumePercent = 12;

        // Also pause media
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlaying = false;
        }

        updateFlashUI("Night Guard");
        updateFanButton();
        updateTvButton();
        updateVolumeUI("Night Guard");
        updateCreativeUI("Night Guard active: all devices OFF");
        updateMediaPlaybackUI();

        tvGestureState.setText("FACE DOWN -> NIGHT GUARD");
        pulseGestureLabel();
        dispatchCommand("SCENE_NIGHT_GUARD", "creative macro");
    }

    private void toggleFlash(String source) {
        flashOn = !flashOn;
        setFlashlight(flashOn);
        updateFlashUI(source);
        dispatchCommand(flashOn ? "FLASH_ON" : "FLASH_OFF", source);
    }

    /**
     * Controls the real phone flashlight (camera flash LED).
     */
    private void setFlashlight(boolean on) {
        if (cameraManager == null || cameraId == null) {
            appendLog("Flashlight not available on this device.");
            return;
        }
        try {
            cameraManager.setTorchMode(cameraId, on);
        } catch (CameraAccessException e) {
            appendLog("Flash error: " + e.getMessage());
        }
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

    private void updateFlashUI(String source) {
        String value = flashOn ? "ON" : "OFF";
        tvLightState.setText("Flash: " + value + "  [" + source + "]");
        tvLightState.setTextColor(ContextCompat.getColor(this, flashOn ? R.color.matrix_green : R.color.matrix_green_dim));
        btnLightManual.setText(flashOn ? "FORCE FLASH OFF" : "FORCE FLASH ON");

        // Update flash icon and label
        tvFlashOnOff.setText(flashOn ? "ON" : "OFF");
        tvFlashOnOff.setTextColor(ContextCompat.getColor(this,
                flashOn ? R.color.light_on_glow : R.color.matrix_green_dim));

        // Animate the flash icon on state change
        tvFlashIcon.animate()
                .scaleX(flashOn ? 1.25f : 0.9f)
                .scaleY(flashOn ? 1.25f : 0.9f)
                .setDuration(200L)
                .withEndAction(() -> tvFlashIcon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();

        // Animate the ON/OFF label
        tvFlashOnOff.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150L)
                .withEndAction(() -> tvFlashOnOff.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300L)
                        .start())
                .start();

        // Change container background
        flashContainer.setBackgroundColor(ContextCompat.getColor(this,
                flashOn ? R.color.light_on_bg : R.color.light_off_bg));
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
        tvMediaTrackName.setText(mediaPlaylist[mediaIndex]);

        // Animate track name change
        tvMediaTrackName.animate()
                .alpha(0.3f)
                .setDuration(100L)
                .withEndAction(() -> tvMediaTrackName.animate()
                        .alpha(1f)
                        .setDuration(300L)
                        .start())
                .start();

        updateMediaPlaybackUI();
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

    // ──────────────────────────────────────────────
    // Media Player methods
    // ──────────────────────────────────────────────

    private void initMediaPlayer() {
        // Use built-in ringtone/notification sounds as demo tracks
        mediaResIds = new int[]{
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.hashCode(),
                android.provider.Settings.System.DEFAULT_RINGTONE_URI.hashCode(),
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.hashCode(),
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.hashCode()
        };

        // Create initial media player with a default system ringtone
        try {
            mediaPlayer = MediaPlayer.create(this,
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setOnCompletionListener(mp -> {
                    // Auto-advance to next track if not looping
                    switchTrack(true);
                });
                // Start playing immediately
                mediaPlayer.start();
                mediaPlaying = true;
            }
        } catch (Exception e) {
            appendLog("Media init error: " + e.getMessage());
            mediaPlaying = false;
        }

        // Progress updater runnable
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlaying) {
                    try {
                        int duration = mediaPlayer.getDuration();
                        int position = mediaPlayer.getCurrentPosition();
                        if (duration > 0) {
                            int progress = (int) ((position / (float) duration) * 100);
                            mediaProgressBar.setProgress(progress);
                        }
                    } catch (Exception ignored) {
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void switchTrack(boolean next) {
        mediaIndex = next
                ? (mediaIndex + 1) % mediaPlaylist.length
                : (mediaIndex - 1 + mediaPlaylist.length) % mediaPlaylist.length;

        // Restart media player with new "track" (using different system sounds)
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // Use different system URIs for each track to simulate different songs
            android.net.Uri trackUri;
            switch (mediaIndex) {
                case 0:
                    trackUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case 1:
                    trackUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI;
                    break;
                case 2:
                    trackUri = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
                    break;
                default:
                    trackUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
            }

            mediaPlayer = MediaPlayer.create(this, trackUri);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                if (mediaPlaying) {
                    mediaPlayer.start();
                }
            }
        } catch (Exception e) {
            appendLog("Track switch error: " + e.getMessage());
        }

        updateMediaUI(next ? "next" : "prev");
        appendLog("Track changed: " + mediaPlaylist[mediaIndex]);
    }

    private void toggleMediaPlayback() {
        if (mediaPlayer == null) {
            appendLog("No media player available.");
            return;
        }

        if (mediaPlaying) {
            mediaPlayer.pause();
            mediaPlaying = false;
            appendLog("Media paused: " + mediaPlaylist[mediaIndex]);
        } else {
            mediaPlayer.start();
            mediaPlaying = true;
            appendLog("Media playing: " + mediaPlaylist[mediaIndex]);
        }

        updateMediaPlaybackUI();
    }

    private void updateMediaPlaybackUI() {
        if (mediaPlaying) {
            btnMediaPlayPause.setText("⏸ PAUSE");
            tvMediaPlayStatus.setText("▶ PLAYING");
            tvMediaPlayStatus.setTextColor(ContextCompat.getColor(this, R.color.matrix_green));
        } else {
            btnMediaPlayPause.setText("▶ PLAY");
            tvMediaPlayStatus.setText("⏸ PAUSED");
            tvMediaPlayStatus.setTextColor(ContextCompat.getColor(this, R.color.state_red));
        }
    }
}
