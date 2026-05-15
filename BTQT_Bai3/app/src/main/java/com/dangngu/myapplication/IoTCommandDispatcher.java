package com.dangngu.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IoTCommandDispatcher {

    public enum Channel {
        WIFI,
        BLUETOOTH
    }

    public interface DispatchCallback {
        void onComplete(boolean success, @NonNull String message);
    }

    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private Channel channel = Channel.WIFI;
    private String wifiEndpoint = "";
    private String bluetoothAddress = "";

    private BluetoothSocket bluetoothSocket;
    private OutputStream bluetoothOutputStream;

    @SuppressWarnings("deprecation")
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @SuppressWarnings("unused")
    public IoTCommandDispatcher(Context context) {
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setWifiEndpoint(String wifiEndpoint) {
        this.wifiEndpoint = wifiEndpoint == null ? "" : wifiEndpoint.trim();
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress == null ? "" : bluetoothAddress.trim();
    }

    public boolean hasBluetoothSupport() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isBluetoothConnected() {
        BluetoothSocket local = bluetoothSocket;
        return local != null && local.isConnected();
    }

    public void connectBluetooth(DispatchCallback callback) {
        ioExecutor.execute(() -> {
            if (bluetoothAdapter == null) {
                post(callback, false, "Thiet bi khong ho tro Bluetooth.");
                return;
            }
            if (!bluetoothAdapter.isEnabled()) {
                post(callback, false, "Bluetooth dang tat.");
                return;
            }

            String targetAddress = bluetoothAddress;
            if (targetAddress.isEmpty()) {
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                if (bondedDevices != null && !bondedDevices.isEmpty()) {
                    targetAddress = bondedDevices.iterator().next().getAddress();
                }
            }

            if (targetAddress == null || targetAddress.trim().isEmpty()) {
                post(callback, false, "Chua co MAC Bluetooth. Nhap dia chi de ket noi.");
                return;
            }

            try {
                disconnectBluetoothInternal();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(targetAddress.trim());
                bluetoothAdapter.cancelDiscovery();
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                bluetoothSocket = socket;
                bluetoothOutputStream = socket.getOutputStream();
                bluetoothAddress = targetAddress.trim();
                post(callback, true, "Bluetooth da ket noi: " + bluetoothAddress);
            } catch (Exception ex) {
                disconnectBluetoothInternal();
                post(callback, false, "Bluetooth loi: " + ex.getMessage());
            }
        });
    }

    public void disconnectBluetooth(DispatchCallback callback) {
        ioExecutor.execute(() -> {
            disconnectBluetoothInternal();
            post(callback, true, "Bluetooth da ngat ket noi.");
        });
    }

    public void dispatchCommand(String command, DispatchCallback callback) {
        if (channel == Channel.BLUETOOTH) {
            sendBluetooth(command, callback);
        } else {
            sendWifi(command, callback);
        }
    }

    private void sendBluetooth(String command, DispatchCallback callback) {
        ioExecutor.execute(() -> {
            if (!isBluetoothConnected() || bluetoothOutputStream == null) {
                post(callback, false, "Bluetooth chua ket noi.");
                return;
            }
            try {
                bluetoothOutputStream.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                bluetoothOutputStream.flush();
                post(callback, true, "Bluetooth > " + command);
            } catch (Exception ex) {
                disconnectBluetoothInternal();
                post(callback, false, "Gui Bluetooth that bai: " + ex.getMessage());
            }
        });
    }

    private void sendWifi(String command, DispatchCallback callback) {
        ioExecutor.execute(() -> {
            if (wifiEndpoint.isEmpty()) {
                post(callback, false, "Chua co URL WiFi endpoint.");
                return;
            }

            HttpURLConnection connection = null;
            try {
                URL endpoint = new URL(normalizeUrl(wifiEndpoint));
                connection = (HttpURLConnection) endpoint.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(3500);
                connection.setReadTimeout(3500);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                String payload = "{\"command\":\"" + command + "\",\"ts\":" + System.currentTimeMillis() + "}";
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    post(callback, true, "WiFi > " + command + " (" + responseCode + ")");
                } else {
                    post(callback, false, "WiFi loi HTTP " + responseCode);
                }
            } catch (Exception ex) {
                post(callback, false, "WiFi loi: " + ex.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String normalizeUrl(String raw) {
        String value = raw.trim();
        if (!(value.startsWith("http://") || value.startsWith("https://"))) {
            value = "http://" + value;
        }
        return value;
    }

    private void disconnectBluetoothInternal() {
        if (bluetoothOutputStream != null) {
            try {
                bluetoothOutputStream.close();
            } catch (IOException ignored) {
            }
            bluetoothOutputStream = null;
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException ignored) {
            }
            bluetoothSocket = null;
        }
    }

    private void post(DispatchCallback callback, boolean success, String message) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onComplete(success, message);
            }
        });
    }

    public void release() {
        ioExecutor.execute(this::disconnectBluetoothInternal);
        ioExecutor.shutdownNow();
    }
}
