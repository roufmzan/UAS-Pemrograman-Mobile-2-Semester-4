package com.example.tugasku;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    
    private FusedLocationProviderClient fusedLocationClient;
    private TextView locationText;
    private ImageView flagImage;
    private TextView appNameText;
    private TextView versionText;
    private TextView poweredByText;
    private boolean locationPermissionGranted = false;
    private LocationCallback locationCallback;
    private boolean isLocationReceived = false;

    private static final String PREFS_NAME = "TugaskuPreferences";
    private static final String PREF_KEY_COUNTRY_CODE = "country_code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String initialCountryCode = getInitialCountryCode();
        applyLocaleForCountry(initialCountryCode);
        
        // Hide system UI for full screen
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_splash);
        
        locationText = findViewById(R.id.location_text);
        flagImage = findViewById(R.id.flag_indonesia);
        appNameText = findViewById(R.id.app_name);
        versionText = findViewById(R.id.version);
        poweredByText = findViewById(R.id.powered_by);
        updateFlagForCountry(initialCountryCode);
        refreshStaticTexts();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Request location permission
        requestLocationPermission();
    }

    private String getInitialCountryCode() {
        String saved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_KEY_COUNTRY_CODE, null);
        if (saved != null && !saved.trim().isEmpty()) return saved;
        return getDeviceCountryCode();
    }

    private String getDeviceCountryCode() {
        String country = Locale.getDefault().getCountry();
        return country != null ? country : "";
    }

    private void persistCountryCode(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) return;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_COUNTRY_CODE, countryCode.toUpperCase(Locale.ROOT))
                .apply();
    }

    private void applyLocaleForCountry(String countryCode) {
        String languageTag = getLanguageTagForCountry(countryCode);
        LocaleListCompat locales = LocaleListCompat.forLanguageTags(languageTag);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    private String getLanguageTagForCountry(String countryCode) {
        if (countryCode == null) return "en";
        switch (countryCode.toUpperCase(Locale.ROOT)) {
            case "ID":
                return "id";
            case "CN":
            case "SG":
                return "zh-CN";
            case "ES":
            case "MX":
            case "AR":
            case "CO":
            case "CL":
            case "PE":
                return "es";
            case "RU":
                return "ru";
            case "PT":
                return "pt";
            case "BR":
                return "pt-BR";
            case "JP":
                return "ja";
            case "FR":
            case "BE":
            case "CH":
            case "CA":
                return "fr";
            case "DE":
            case "AT":
                return "de";
            case "US":
            case "GB":
            case "AU":
            default:
                return "en";
        }
    }

    private void updateFlagForCountry(String countryCode) {
        if (flagImage == null) return;
        String cc = countryCode != null ? countryCode.toLowerCase(Locale.ROOT) : "";
        int resId = getResources().getIdentifier("flag_" + cc, "drawable", getPackageName());
        if (resId == 0) {
            String langTag = getLanguageTagForCountry(countryCode);
            String lang = langTag != null ? langTag.toLowerCase(Locale.ROOT) : "";
            String fallbackName;
            if (lang.startsWith("zh")) {
                fallbackName = "flag_cn";
            } else {
                int dash = lang.indexOf('-');
                String primary = dash > 0 ? lang.substring(0, dash) : lang;
                fallbackName = "flag_" + primary;
            }
            resId = getResources().getIdentifier(fallbackName, "drawable", getPackageName());
        }
        if (resId == 0) {
            resId = R.drawable.flag_indonesia;
        }
        flagImage.setImageResource(resId);
    }

    private void applyCountryConfig(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) return;
        String normalized = countryCode.toUpperCase(Locale.ROOT);
        persistCountryCode(normalized);
        applyLocaleForCountry(normalized);
        updateFlagForCountry(normalized);
        refreshStaticTexts();
    }

    private void refreshStaticTexts() {
        if (appNameText != null) appNameText.setText(R.string.splash_app_name);
        if (versionText != null) versionText.setText(R.string.version_text);
        if (poweredByText != null) poweredByText.setText(R.string.powered_by);
    }
    
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted
            locationPermissionGranted = true;
            getLocationAndProceed();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                getLocationAndProceed();
            } else {
                // Permission denied, proceed without location
                locationText.setText(R.string.location_unavailable);
                applyCountryConfig(getDeviceCountryCode());
                proceedToMainActivity();
            }
        }
    }
    
    private void getLocationAndProceed() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationText.setText(R.string.location_unavailable);
            applyCountryConfig(getDeviceCountryCode());
            proceedToMainActivity();
            return;
        }
        
        // Check if GPS is enabled
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            // GPS is disabled, show dialog
            showGPSDisabledDialog();
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            getAddressFromLocation(location);
                        } else {
                            // Try to request current location if last location is null
                            requestCurrentLocation();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        locationText.setText(R.string.location_unavailable);
                        applyCountryConfig(getDeviceCountryCode());
                        proceedToMainActivity();
                    }
                });
    }
    
    private void showGPSDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gps_disabled_title);
        builder.setMessage(R.string.gps_disabled_message);
        builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Open location settings
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                locationText.setText(R.string.location_unavailable);
                applyCountryConfig(getDeviceCountryCode());
                proceedToMainActivity();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                locationText.setText(R.string.location_unavailable);
                applyCountryConfig(getDeviceCountryCode());
                proceedToMainActivity();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    private void requestCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationText.setText(R.string.location_unavailable);
            applyCountryConfig(getDeviceCountryCode());
            proceedToMainActivity();
            return;
        }
        
        // Create location request
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(5000)
                .build();
        
        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null && !isLocationReceived) {
                    isLocationReceived = true;
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        // Stop location updates
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                        getAddressFromLocation(location);
                    } else {
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                        locationText.setText("Lokasi tidak tersedia");
                        proceedToMainActivity();
                    }
                }
            }
        };
        
        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        locationText.setText(R.string.location_unavailable);
                        applyCountryConfig(getDeviceCountryCode());
                        proceedToMainActivity();
                    }
                });
        
        // Set timeout to prevent waiting forever
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isLocationReceived) {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                    locationText.setText(R.string.location_unavailable);
                    applyCountryConfig(getDeviceCountryCode());
                    if (!isFinishing()) {
                        proceedToMainActivity();
                    }
                }
            }
        }, 8000); // 8 seconds timeout
    }
    
    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                if (address.getCountryCode() != null) {
                    applyCountryConfig(address.getCountryCode());
                } else {
                    applyCountryConfig(getDeviceCountryCode());
                }
                
                // Build location string
                StringBuilder locationString = new StringBuilder();
                
                // Country
                if (address.getCountryName() != null) {
                    locationString.append(address.getCountryName());
                }
                
                // Admin area (Province/State)
                if (address.getAdminArea() != null) {
                    if (locationString.length() > 0) locationString.append(", ");
                    locationString.append(address.getAdminArea());
                }
                
                // Sub admin area (Regency/City)
                if (address.getSubAdminArea() != null) {
                    if (locationString.length() > 0) locationString.append(", ");
                    locationString.append(address.getSubAdminArea());
                }
                
                // Locality (District)
                if (address.getLocality() != null) {
                    if (locationString.length() > 0) locationString.append(", ");
                    locationString.append(address.getLocality());
                }
                
                // Sub locality (Village/Neighborhood)
                if (address.getSubLocality() != null) {
                    if (locationString.length() > 0) locationString.append(", ");
                    locationString.append(address.getSubLocality());
                }
                
                locationText.setText(locationString.toString());
            } else {
                locationText.setText(R.string.country_default_name);
                applyCountryConfig(getDeviceCountryCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
            locationText.setText(R.string.country_default_name);
            applyCountryConfig(getDeviceCountryCode());
        }
        
        proceedToMainActivity();
    }
    
    private void proceedToMainActivity() {
        // Navigate to MainActivity after splash duration
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove location updates if still active
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
