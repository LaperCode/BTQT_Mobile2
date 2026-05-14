package com.example.ex_2_the_weather_app;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddressRepository {
    private static final String TAG = "AddressRepository";
    private final Geocoder geocoder;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AddressCallback {
        void onSuccess(String address);
        void onError(String message);
    }

    public AddressRepository(Context context) {
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    public void getAddress(double latitude, double longitude, AddressCallback callback) {
        new Thread(() -> {
            try {
                if (!Geocoder.isPresent()) {
                    Log.d(TAG, "Geocoder not available, fallback to coordinates");
                    mainHandler.post(() -> callback.onError("Geocoder not available"));
                    return;
                }

                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String addressText = formatAddress(address);
                    Log.d(TAG, "Address found: " + addressText);
                    mainHandler.post(() -> callback.onSuccess(addressText));
                } else {
                    Log.d(TAG, "No address found for coordinates");
                    mainHandler.post(() -> callback.onError("No address found"));
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();

        String addressLine0 = address.getMaxAddressLineIndex() >= 0 ? address.getAddressLine(0) : null;
        String subAdminArea = address.getSubAdminArea();
        String adminArea = address.getAdminArea();
        String locality = address.getLocality();
        String countryName = address.getCountryName();

        // Add street/ward info if available
        if (addressLine0 != null && !addressLine0.isEmpty() && !addressLine0.contains(countryName != null ? countryName : "")) {
            sb.append(addressLine0);
        }

        // Add ward/district if available and not already in addressLine0
        if (subAdminArea != null && !subAdminArea.isEmpty() && (addressLine0 == null || !addressLine0.contains(subAdminArea))) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(subAdminArea);
        }

        // Add city (locality) only if not already added
        if (locality != null && !locality.isEmpty() && (addressLine0 == null || !addressLine0.contains(locality))) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(locality);
        }

        // Add country only once at the end
        if (countryName != null && !countryName.isEmpty() && (sb.indexOf(countryName) == -1)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(countryName);
        }

        return sb.length() > 0 ? sb.toString() : "Vị trí không xác định";
    }
}
