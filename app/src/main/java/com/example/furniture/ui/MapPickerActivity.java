package com.example.furniture.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.LanguageManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MapPickerActivity — pemilih lokasi berbasis OpenStreetMap (OSMDroid).
 *
 * Fitur:
 *   - Peta full-screen dengan pin statis di tengah
 *   - Auto-center ke GPS saat dibuka
 *   - Reverse geocoding saat peta digeser (via Nominatim)
 *   - Search bar untuk cari lokasi (via Nominatim)
 *   - Tombol "Pilih Lokasi Ini" untuk confirm dan kirim data kembali
 *
 * Dipanggil via startActivityForResult dari CheckoutActivity atau SettingsFragment.
 * Mengembalikan RESULT_OK dengan extras:
 *   - Constants.EXTRA_SELECTED_ADDRESS (String)
 *   - Constants.EXTRA_SELECTED_LAT (double)
 *   - Constants.EXTRA_SELECTED_LNG (double)
 */
public class MapPickerActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERM = 500;

    // ─── Views ───────────────────────────────────────────────────────────────────

    private MapView mapView;
    private EditText etSearch;
    private TextView tvAddressTitle;
    private TextView tvAddressDetail;
    private ProgressBar progressGeocode;
    private MaterialCardView cardSearchResults;
    private RecyclerView rvSearchResults;
    private FloatingActionButton fabMyLocation;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler geocodeDebounce = new Handler(Looper.getMainLooper());
    private Runnable pendingGeocode;

    private String currentAddress = "";
    private double currentLat = 0;
    private double currentLng = 0;
    private boolean initialCenterDone = false;

    private final List<SearchResult> searchResults = new ArrayList<>();
    private SearchResultAdapter searchAdapter;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Konfigurasi OSMDroid (wajib sebelum inflate layout)
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map_picker);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        initMap();
        initSearch();
        initLocation();
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
        executor.shutdown();
        geocodeDebounce.removeCallbacksAndMessages(null);
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initViews() {
        mapView          = findViewById(R.id.map_view);
        etSearch         = findViewById(R.id.et_map_search);
        tvAddressTitle   = findViewById(R.id.tv_address_title);
        tvAddressDetail  = findViewById(R.id.tv_address_detail);
        progressGeocode  = findViewById(R.id.progress_geocode);
        cardSearchResults = findViewById(R.id.card_search_results);
        rvSearchResults  = findViewById(R.id.rv_search_results);
        fabMyLocation    = findViewById(R.id.fab_my_location);

        ImageButton btnBack = findViewById(R.id.btn_back_map);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Confirm button
        View btnConfirm = findViewById(R.id.btn_confirm_location);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> confirmLocation());
        }

        // My Location FAB
        if (fabMyLocation != null) {
            fabMyLocation.setOnClickListener(v -> requestAndCenterToGps());
        }

        // Search results RecyclerView
        searchAdapter = new SearchResultAdapter();
        if (rvSearchResults != null) {
            rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
            rvSearchResults.setAdapter(searchAdapter);
        }
    }

    /**
     * Inisialisasi OSM MapView.
     */
    private void initMap() {
        if (mapView == null) return;

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(Constants.DEFAULT_MAP_ZOOM);

        // Posisi default: Jakarta (akan dioverride GPS)
        GeoPoint defaultPoint = new GeoPoint(-6.2088, 106.8456);
        mapView.getController().setCenter(defaultPoint);

        // Listener saat peta digeser → reverse geocode koordinat tengah
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                scheduleReverseGeocode();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                scheduleReverseGeocode();
                return true;
            }
        });
    }

    /**
     * Setup search bar dengan debounce.
     */
    private void initSearch() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            private Runnable runnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (runnable != null) handler.removeCallbacks(runnable);
                runnable = () -> {
                    String query = s.toString().trim();
                    if (query.length() >= 3) {
                        searchLocation(query);
                    } else {
                        hideSearchResults();
                    }
                };
                handler.postDelayed(runnable, 500); // Debounce 500ms (Nominatim policy)
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (query.length() >= 3) {
                    searchLocation(query);
                }
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    /**
     * Inisialisasi FusedLocationProviderClient dan auto-center ke GPS.
     */
    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestAndCenterToGps();
    }

    // ─── GPS ─────────────────────────────────────────────────────────────────────

    private void requestAndCenterToGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            centerToCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERM);
        }
    }

    private void centerToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        animateToLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        // Fallback ke last location
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(this, lastLocation -> {
                                    if (lastLocation != null) {
                                        animateToLocation(lastLocation.getLatitude(),
                                                lastLocation.getLongitude());
                                    }
                                });
                    }
                });
    }

    private void animateToLocation(double lat, double lng) {
        if (mapView == null) return;
        GeoPoint point = new GeoPoint(lat, lng);
        mapView.getController().animateTo(point);
        if (!initialCenterDone) {
            mapView.getController().setZoom(Constants.DEFAULT_MAP_ZOOM);
            initialCenterDone = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centerToCurrentLocation();
            } else {
                Toast.makeText(this, getString(R.string.location_permission_required),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ─── Reverse Geocode (Map Center → Address) ─────────────────────────────────

    /**
     * Debounce reverse geocoding: tunggu 300ms setelah user berhenti scroll.
     */
    private void scheduleReverseGeocode() {
        if (pendingGeocode != null) {
            geocodeDebounce.removeCallbacks(pendingGeocode);
        }
        pendingGeocode = () -> {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            reverseGeocode(center.getLatitude(), center.getLongitude());
        };
        geocodeDebounce.postDelayed(pendingGeocode, 300);
    }

    /**
     * Reverse geocoding via Nominatim API.
     * https://nominatim.openstreetmap.org/reverse?lat=...&lon=...&format=json
     */
    private void reverseGeocode(double lat, double lng) {
        mainHandler.post(() -> {
            if (progressGeocode != null) progressGeocode.setVisibility(View.VISIBLE);
            if (tvAddressTitle != null) tvAddressTitle.setText(getString(R.string.map_detecting_address));
            if (tvAddressDetail != null) tvAddressDetail.setText(String.format("%.6f, %.6f", lat, lng));
        });

        String lang = LanguageManager.isIndonesian(this) ? "id" : "en";

        executor.execute(() -> {
            try {
                String urlStr = Constants.NOMINATIM_BASE_URL
                        + "reverse?lat=" + lat
                        + "&lon=" + lng
                        + "&format=json"
                        + "&addressdetails=1"
                        + "&accept-language=" + lang;

                String response = httpGet(urlStr);
                if (response == null) {
                    mainHandler.post(this::showGeocodeError);
                    return;
                }

                JSONObject json = new JSONObject(response);
                String displayName = json.optString("display_name", "");
                JSONObject address = json.optJSONObject("address");

                // Extract komponen alamat
                String road = "";
                String city = "";
                String state = "";
                String postcode = "";
                String country = "";

                if (address != null) {
                    road = firstNonEmpty(address, "road", "pedestrian", "suburb", "neighbourhood", "village");
                    city = firstNonEmpty(address, "city", "town", "municipality", "county", "city_district");
                    state = address.optString("state", "");
                    postcode = address.optString("postcode", "");
                    country = address.optString("country", "");
                }

                // Susun alamat ringkas untuk title
                String title = !road.isEmpty() ? road : (!city.isEmpty() ? city : displayName);
                // Detail: city, state, postcode
                StringBuilder detail = new StringBuilder();
                if (!city.isEmpty()) detail.append(city);
                if (!state.isEmpty()) {
                    if (detail.length() > 0) detail.append(", ");
                    detail.append(state);
                }
                if (!postcode.isEmpty()) {
                    if (detail.length() > 0) detail.append(" ");
                    detail.append(postcode);
                }
                if (!country.isEmpty()) {
                    if (detail.length() > 0) detail.append(", ");
                    detail.append(country);
                }

                // Alamat lengkap untuk dikirim kembali
                String fullAddress = buildFullAddress(road, city, state, postcode, country);

                final String fTitle = title;
                final String fDetail = detail.toString();
                final String fFull = fullAddress;

                mainHandler.post(() -> {
                    currentAddress = fFull;
                    currentLat = lat;
                    currentLng = lng;
                    if (tvAddressTitle != null) tvAddressTitle.setText(fTitle);
                    if (tvAddressDetail != null) tvAddressDetail.setText(fDetail);
                    if (progressGeocode != null) progressGeocode.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                mainHandler.post(this::showGeocodeError);
            }
        });
    }

    private void showGeocodeError() {
        if (progressGeocode != null) progressGeocode.setVisibility(View.GONE);
        if (tvAddressTitle != null) tvAddressTitle.setText(getString(R.string.geocode_failed));
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        currentLat = center.getLatitude();
        currentLng = center.getLongitude();
        currentAddress = String.format("%.6f, %.6f", currentLat, currentLng);
    }

    // ─── Search (Nominatim Forward Geocoding) ───────────────────────────────────

    /**
     * Cari lokasi via Nominatim search API.
     */
    private void searchLocation(String query) {
        String lang = LanguageManager.isIndonesian(this) ? "id" : "en";

        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String urlStr = Constants.NOMINATIM_BASE_URL
                        + "search?q=" + encoded
                        + "&format=json"
                        + "&limit=5"
                        + "&accept-language=" + lang;

                String response = httpGet(urlStr);
                if (response == null) {
                    mainHandler.post(this::hideSearchResults);
                    return;
                }

                JSONArray array = new JSONArray(response);
                List<SearchResult> results = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    results.add(new SearchResult(
                            obj.optString("display_name", ""),
                            obj.optDouble("lat", 0),
                            obj.optDouble("lon", 0)
                    ));
                }

                mainHandler.post(() -> {
                    searchResults.clear();
                    searchResults.addAll(results);
                    searchAdapter.notifyDataSetChanged();
                    if (!results.isEmpty()) {
                        showSearchResults();
                    } else {
                        hideSearchResults();
                    }
                });

            } catch (Exception e) {
                mainHandler.post(this::hideSearchResults);
            }
        });
    }

    private void showSearchResults() {
        if (cardSearchResults != null) cardSearchResults.setVisibility(View.VISIBLE);
    }

    private void hideSearchResults() {
        if (cardSearchResults != null) cardSearchResults.setVisibility(View.GONE);
    }

    private void onSearchResultSelected(SearchResult result) {
        hideSearchResults();
        hideKeyboard();

        // Set search text tanpa trigger TextWatcher
        if (etSearch != null) {
            etSearch.removeTextChangedListener(null);
            etSearch.setText("");
        }

        animateToLocation(result.lat, result.lng);
    }

    // ─── Confirm ─────────────────────────────────────────────────────────────────

    private void confirmLocation() {
        if (TextUtils.isEmpty(currentAddress)) {
            Toast.makeText(this, getString(R.string.map_wait_address), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_SELECTED_ADDRESS, currentAddress);
        result.putExtra(Constants.EXTRA_SELECTED_LAT, currentLat);
        result.putExtra(Constants.EXTRA_SELECTED_LNG, currentLng);
        setResult(RESULT_OK, result);
        finish();
    }

    // ─── HTTP Helper ─────────────────────────────────────────────────────────────

    /**
     * Simple HTTP GET. Mengembalikan response body sebagai String, atau null jika gagal.
     * Menggunakan HttpURLConnection agar tidak perlu dependency tambahan.
     */
    private String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", getPackageName());
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            if (code != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();

        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Ambil nilai non-kosong pertama dari JSONObject berdasarkan urutan key.
     */
    private String firstNonEmpty(JSONObject obj, String... keys) {
        for (String key : keys) {
            String val = obj.optString(key, "");
            if (!val.isEmpty()) return val;
        }
        return "";
    }

    /**
     * Susun alamat lengkap dari komponen, dipisah koma.
     */
    private String buildFullAddress(String road, String city, String state,
                                    String postcode, String country) {
        StringBuilder sb = new StringBuilder();
        if (!road.isEmpty()) sb.append(road);
        if (!city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (!state.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (!postcode.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(postcode);
        }
        if (!country.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (imm != null && focused != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    // ─── Inner: Search Result Model ─────────────────────────────────────────────

    private static class SearchResult {
        final String displayName;
        final double lat;
        final double lng;

        SearchResult(String displayName, double lat, double lng) {
            this.displayName = displayName;
            this.lat = lat;
            this.lng = lng;
        }
    }

    // ─── Inner: Search Result Adapter ───────────────────────────────────────────

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_result, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchResult item = searchResults.get(position);
            holder.tvName.setText(item.displayName);
            holder.itemView.setOnClickListener(v -> onSearchResultSelected(item));
        }

        @Override
        public int getItemCount() {
            return searchResults.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;

            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_search_result_name);
            }
        }
    }
}
