package com.example.furniture.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.furniture.R;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.LanguageManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderTrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private Marker courierMarker;
    private GeoPoint warehousePoint;
    private GeoPoint destinationPoint;

    // Dummy Warehouse coordinate (Central Jakarta)
    private static final double WAREHOUSE_LAT = -6.175110;
    private static final double WAREHOUSE_LNG = 106.827152;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_order_tracking);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mapView = findViewById(R.id.map_tracking);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        ImageButton btnBack = findViewById(R.id.btn_back_tracking);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        FloatingActionButton fabCenter = findViewById(R.id.fab_center_courier);
        if (fabCenter != null) {
            fabCenter.setOnClickListener(v -> {
                if (courierMarker != null) {
                    mapView.getController().animateTo(courierMarker.getPosition());
                }
            });
        }

        // Default warehouse
        warehousePoint = new GeoPoint(WAREHOUSE_LAT, WAREHOUSE_LNG);

        // Get Address from intent
        Intent intent = getIntent();
        String addressStr = intent.getStringExtra(Constants.EXTRA_SELECTED_ADDRESS);
        
        if (TextUtils.isEmpty(addressStr)) {
            // Fallback destination (South Jakarta)
            setupTracking(new GeoPoint(-6.261493, 106.810600));
        } else {
            geocodeAddress(addressStr);
        }
    }

    private void geocodeAddress(String addressStr) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("id", "ID"));
                List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
                GeoPoint destPoint;
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    destPoint = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                } else {
                    // Fallback
                    destPoint = new GeoPoint(-6.261493, 106.810600);
                }
                
                GeoPoint finalDestPoint = destPoint;
                runOnUiThread(() -> setupTracking(finalDestPoint));
                
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Geocoding failed, using default destination", Toast.LENGTH_SHORT).show();
                    setupTracking(new GeoPoint(-6.261493, 106.810600));
                });
            }
        }).start();
    }

    private void setupTracking(GeoPoint destination) {
        destinationPoint = destination;

        // Warehouse Marker
        Marker whMarker = new Marker(mapView);
        whMarker.setPosition(warehousePoint);
        whMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        whMarker.setIcon(getResources().getDrawable(R.drawable.ic_warehouse));
        whMarker.setTitle("Central Warehouse");
        mapView.getOverlays().add(whMarker);

        // Destination Marker
        Marker destMarker = new Marker(mapView);
        destMarker.setPosition(destinationPoint);
        destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destMarker.setIcon(getResources().getDrawable(R.drawable.ic_map_pin));
        destMarker.setTitle("Your Address");
        mapView.getOverlays().add(destMarker);

        // Draw Route (Polyline)
        Polyline route = new Polyline(mapView);
        List<GeoPoint> routePoints = new ArrayList<>();
        routePoints.add(warehousePoint);
        routePoints.add(destinationPoint); // For a simple straight line
        route.setPoints(routePoints);
        route.setColor(Color.parseColor("#212121"));
        route.setWidth(8f);
        mapView.getOverlays().add(route);

        // Courier Marker (Static near destination to show "almost there")
        courierMarker = new Marker(mapView);
        double endFraction = 0.95; // 95% from warehouse (very close to destination)
        double courierLat = warehousePoint.getLatitude() + (destinationPoint.getLatitude() - warehousePoint.getLatitude()) * endFraction;
        double courierLng = warehousePoint.getLongitude() + (destinationPoint.getLongitude() - warehousePoint.getLongitude()) * endFraction;
        GeoPoint courierPos = new GeoPoint(courierLat, courierLng);
        
        courierMarker.setPosition(courierPos);
        courierMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        courierMarker.setIcon(getResources().getDrawable(R.drawable.ic_truck));
        courierMarker.setTitle("Courier (Almost arriving!)");
        mapView.getOverlays().add(courierMarker);

        // Focus map in the middle of the route so both warehouse and destination are visible
        double centerLat = (warehousePoint.getLatitude() + destinationPoint.getLatitude()) / 2;
        double centerLng = (warehousePoint.getLongitude() + destinationPoint.getLongitude()) / 2;
        GeoPoint centerPoint = new GeoPoint(centerLat, centerLng);
        
        mapView.getController().setZoom(13.0); // Slightly zoomed out to see the whole route
        mapView.getController().setCenter(centerPoint);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
