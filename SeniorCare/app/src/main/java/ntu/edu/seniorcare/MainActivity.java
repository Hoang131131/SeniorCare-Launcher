package ntu.edu.seniorcare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException; // Thêm import này
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings; // Thêm import này
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ntu.edu.seniorcare.apps.AppAdapter;
import ntu.edu.seniorcare.apps.AppInfo;
import ntu.edu.seniorcare.contact.ContactsActivity;
import ntu.edu.seniorcare.settings.SettingsActivity;
import ntu.edu.seniorcare.settings.SettingsUtils;
import ntu.edu.seniorcare.sms.SmsActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String OPENWEATHER_API_KEY = "555f6b582f3ad5b9bf601a9380617851"; // Đảm bảo đã thay thế bằng API key thật
    private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather?";

    // Constants cho SharedPreferences
    private static final String PREFS_NAME = "MyLauncherPrefs";
    private static final String HAS_ASKED_DEFAULT_LAUNCHER = "hasAskedDefaultLauncher";

    private TextView timeTextView;
    private TextView dateTextView;
    private TextView weatherTextView;
    private TextView temperateTextView; // THÊM DÒNG NÀY ĐỂ KẾT NỐI VỚI temperate_text_view
    private AudioManager audioManager;

    private RecyclerView appGridRecyclerView;
    private AppAdapter appAdapter;
    private List<AppInfo> appList;

    private FusedLocationProviderClient fusedLocationClient;
    private OkHttpClient httpClient;
    private ExecutorService weatherExecutorService;

    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_TICK.equals(intent.getAction()) ||
                    Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) ||
                    Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                updateDateTime();
                // Cập nhật thời tiết theo giờ/nửa giờ một lần nếu cần (có thể giới hạn tần suất)
                fetchWeather();
            }
        }
    };

    private BroadcastReceiver settingsUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SettingsActivity.ACTION_UPDATE_LAUNCHER.equals(intent.getAction())) {
                applySettings();
                loadAndDisplayApps();
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeTextView = findViewById(R.id.time_text_view);
        dateTextView = findViewById(R.id.date_text_view);
        weatherTextView = findViewById(R.id.weather_text_view);
        temperateTextView = findViewById(R.id.temperate_text_view); // KHỞI TẠO temperateTextView
        ImageButton volumeUpButton = findViewById(R.id.volume_up_button);
        ImageButton volumeDownButton = findViewById(R.id.volume_down_button);
        appGridRecyclerView = findViewById(R.id.app_grid_recycler_view);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        httpClient = new OkHttpClient();
        weatherExecutorService = Executors.newSingleThreadExecutor();

        updateDateTime();

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction(Intent.ACTION_TIME_TICK);
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timeReceiver, timeFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(timeReceiver, timeFilter);
        }

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        IntentFilter settingsFilter = new IntentFilter(SettingsActivity.ACTION_UPDATE_LAUNCHER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsUpdateReceiver, settingsFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(settingsUpdateReceiver, settingsFilter);
        }

        volumeUpButton.setOnClickListener(v -> adjustVolume(true));
        volumeDownButton.setOnClickListener(v -> adjustVolume(false));

        appList = new ArrayList<>();
        loadAndDisplayApps();
        applySettings();

        // Lấy và hiển thị thời tiết ban đầu
        requestLocationPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndDisplayApps();
        applySettings();
        fetchWeather(); // Cập nhật thời tiết khi ứng dụng trở lại foreground
        checkDefaultLauncher(); // Kiểm tra và hỏi người dùng về launcher mặc định
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(timeReceiver);
        unregisterReceiver(settingsUpdateReceiver);
        weatherExecutorService.shutdownNow();
    }

    private void loadAndDisplayApps() {
        appList.clear();
        PackageManager pm = getPackageManager();

        // Thêm ứng dụng cài đặt mặc định (fixed app)
        AppInfo settingsApp = new AppInfo(
                "Cài đặt",
                getResources().getDrawable(R.drawable.ic_settings, null),
                getPackageName(),
                SettingsActivity.class.getName()
        );
        appList.add(settingsApp);

        String selectedAppsJson = SettingsUtils.getSelectedAppsJson(this);

        if (selectedAppsJson != null && !selectedAppsJson.isEmpty() && !selectedAppsJson.equals("[]")) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<AppInfo>>() {}.getType();
            List<AppInfo> savedApps = gson.fromJson(selectedAppsJson, type);

            if (savedApps != null) {
                for (AppInfo savedApp : savedApps) {
                    // Chỉ bỏ qua nếu là SettingsActivity (fixed app)
                    // SMSActivity và ContactsActivity sẽ được thêm nếu chúng nằm trong savedApps
                    boolean isFixedSettingsApp = (getPackageName().equals(savedApp.getPackageName()) &&
                            (Objects.equals(savedApp.getClassName(), SettingsActivity.class.getName())));
                    if (isFixedSettingsApp) {
                        continue;
                    }

                    Drawable appIcon = null;
                    try {
                        // Xử lý đặc biệt cho các Activity nội bộ (SMS, Contacts, Settings nếu cần)
                        if (Objects.equals(savedApp.getPackageName(), getPackageName()) && savedApp.getClassName() != null) {
                            if (Objects.equals(savedApp.getClassName(), SmsActivity.class.getName())) {
                                appIcon = getResources().getDrawable(R.drawable.ic_message, null);
                            } else if (Objects.equals(savedApp.getClassName(), ContactsActivity.class.getName())) {
                                appIcon = getResources().getDrawable(R.drawable.ic_contacts, null);
                            } else if (Objects.equals(savedApp.getClassName(), SettingsActivity.class.getName())) {
                                appIcon = getResources().getDrawable(R.drawable.ic_settings, null);
                            }
                        }

                        // Nếu không phải internal special case hoặc không tìm thấy drawable đặc biệt
                        if (appIcon == null) {
                            appIcon = pm.getApplicationIcon(savedApp.getPackageName());
                        }

                        savedApp.setAppIcon(appIcon);
                        appList.add(savedApp);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("MainActivity", "App icon not found for package: " + savedApp.getPackageName() + ". Using default.", e);
                        savedApp.setAppIcon(ContextCompat.getDrawable(this, android.R.drawable.sym_def_app_icon));
                        appList.add(savedApp);
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error loading app icon for package: " + savedApp.getPackageName() + ". Using default.", e);
                        savedApp.setAppIcon(ContextCompat.getDrawable(this, android.R.drawable.sym_def_app_icon));
                        appList.add(savedApp);
                    }
                }
            }
        }
        Collections.sort(appList, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));

        if (appAdapter == null) {
            appGridRecyclerView.setLayoutManager(new GridLayoutManager(this, SettingsUtils.getNumColumns(this)));
            appAdapter = new AppAdapter(this, appList);
            appGridRecyclerView.setAdapter(appAdapter);
        } else {
            appAdapter.setAppList(appList);
            appAdapter.notifyDataSetChanged();
        }
    }

    private void applySettings() {
        float iconSizeFactor = (float) SettingsUtils.getIconSizePercentage(this) / 100f;
        float textSizeFactor = (float) SettingsUtils.getTextSizePercentage(this) / 100f;

        if (appAdapter != null) {
            appAdapter.setIconSizeFactor(iconSizeFactor);
            appAdapter.setTextSizeFactor(textSizeFactor);
            appAdapter.notifyDataSetChanged();
        }

        applyGridLayout();
    }

    private void applyGridLayout() {
        int effectiveNumColumns = SettingsUtils.getNumColumns(this);

        if (appGridRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) appGridRecyclerView.getLayoutManager();
            if (layoutManager.getSpanCount() != effectiveNumColumns) {
                layoutManager.setSpanCount(effectiveNumColumns);
                layoutManager.requestLayout();
            }
        } else {
            appGridRecyclerView.setLayoutManager(new GridLayoutManager(this, effectiveNumColumns));
        }
    }

    private void updateDateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = timeFormat.format(calendar.getTime());
        timeTextView.setText(currentTime);

        SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

        String dayOfWeek = dayOfWeekFormat.format(calendar.getTime());
        String date = dateFormat.format(calendar.getTime());

        String formattedDate = capitalizeFirstLetter(dayOfWeek) + "\n" + date;
        dateTextView.setText(formattedDate);
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void adjustVolume(boolean increase) {
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volumeStep = (int) Math.ceil(maxVolume * 0.05);

        int newVolume;
        if (increase) {
            newVolume = Math.min(currentVolume + volumeStep, maxVolume);
        } else {
            newVolume = Math.max(currentVolume - volumeStep, 0);
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI);
        Toast.makeText(this, "Âm lượng: " + (int)((double)newVolume / maxVolume * 100) + "%", Toast.LENGTH_SHORT).show();
    }

    // --- Xử lý quyền vị trí và thời tiết ---
    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Quyền đã được cấp, lấy vị trí
            getLocationAndFetchWeather();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền vị trí được cấp
                getLocationAndFetchWeather();
            } else {
                // Quyền vị trí bị từ chối
                Toast.makeText(this, "Quyền truy cập vị trí bị từ chối. Không thể lấy thông tin thời tiết.", Toast.LENGTH_LONG).show();
                weatherTextView.setText("Thời tiết: N/A");
                temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView KHI KHÔNG CÓ QUYỀN
            }
        }
    }

    @SuppressLint("MissingPermission") // Quyền đã được kiểm tra trong requestLocationPermissions
    private void getLocationAndFetchWeather() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Vị trí: " + location.getLatitude() + ", " + location.getLongitude());
                        fetchWeatherFromApi(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.w(TAG, "Không thể lấy vị trí cuối cùng.");
                        weatherTextView.setText("Thời tiết: N/A");
                        temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView
                        Toast.makeText(this, "Không thể xác định vị trí hiện tại.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Lỗi khi lấy vị trí: " + e.getMessage());
                    weatherTextView.setText("Thời tiết: N/A");
                    temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView
                    Toast.makeText(this, "Lỗi khi lấy vị trí.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchWeather() {
        // Chỉ gọi lại khi đã có quyền
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocationAndFetchWeather();
        } else {
            // Nếu chưa có quyền, sẽ yêu cầu lại hoặc hiển thị N/A
            weatherTextView.setText("Thời tiết: N/A");
            temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView
        }
    }


    private void fetchWeatherFromApi(double lat, double lon) {
        if (OPENWEATHER_API_KEY.equals("YOUR_OPENWEATHERMAP_API_KEY")) {
            Log.e(TAG, "Vui lòng thay thế YOUR_OPENWEATHERMAP_API_KEY bằng API key thực của bạn.");
            runOnUiThread(() -> {
                weatherTextView.setText("Lỗi API Key");
                temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView
            });
            return;
        }

        String url = WEATHER_API_URL + "lat=" + lat + "&lon=" + lon + "&appid=" + OPENWEATHER_API_KEY + "&units=metric&lang=vi";
        Log.d(TAG, "Gọi API thời tiết: " + url);

        Request request = new Request.Builder().url(url).build();

        weatherExecutorService.execute(() -> {
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Lỗi khi gọi API thời tiết: " + e.getMessage());
                    runOnUiThread(() -> {
                        weatherTextView.setText("Lỗi mạng");
                        temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        Log.d(TAG, "Dữ liệu thời tiết: " + responseData);
                        Gson gson = new Gson();
                        WeatherResponse weatherResponse = gson.fromJson(responseData, WeatherResponse.class);

                        if (weatherResponse != null && weatherResponse.main != null && weatherResponse.weather != null && !weatherResponse.weather.isEmpty()) {
                            final String temperature = String.format(Locale.getDefault(), "%.0f°C", weatherResponse.main.temp);
                            final String description = capitalizeFirstLetter(weatherResponse.weather.get(0).description);

                            runOnUiThread(() -> {
                                temperateTextView.setText(temperature); // CẬP NHẬT temperateTextView
                                weatherTextView.setText(description); // CẬP NHẬT weatherTextView
                            });
                        } else {
                            Log.e(TAG, "Dữ liệu thời tiết không hợp lệ.");
                            runOnUiThread(() -> {
                                weatherTextView.setText("Lỗi dữ liệu");
                                temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView
                            });
                        }
                    } else {
                        Log.e(TAG, "Phản hồi API thời tiết không thành công: " + response.code() + " - " + response.message());
                        runOnUiThread(() -> {
                            weatherTextView.setText("Lỗi");
                            temperateTextView.setText("N/A"); // ĐẶT LẠI temperateTextView
                        });
                    }
                }
            });
        });
    }

    // --- Các lớp POJO cho việc parse JSON từ OpenWeatherMap ---
    private static class WeatherResponse {
        @SerializedName("weather")
        List<Weather> weather;
        @SerializedName("main")
        Main main;
    }

    private static class Weather {
        @SerializedName("description")
        String description;
    }

    private static class Main {
        @SerializedName("temp")
        double temp;
    }

    // --- Phương thức kiểm tra và hỏi về launcher mặc định ---
    private void checkDefaultLauncher() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasAsked = prefs.getBoolean(HAS_ASKED_DEFAULT_LAUNCHER, false);

        if (!hasAsked && !isMyLauncherDefault()) {
            showSetDefaultLauncherDialog();
            prefs.edit().putBoolean(HAS_ASKED_DEFAULT_LAUNCHER, true).apply(); // Đánh dấu đã hỏi
        }
    }

    private boolean isMyLauncherDefault() {
        // Tạo Intent mà Android sử dụng để tìm launcher mặc định
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        // Lấy thông tin về Activity sẽ xử lý Intent này MẶC ĐỊNH
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        // Nếu có một Activity xử lý Intent này và package của nó trùng với package của ứng dụng của bạn,
        // thì ứng dụng của bạn là launcher mặc định.
        return resolveInfo != null && getPackageName().equals(resolveInfo.activityInfo.packageName);
    }


    private void showSetDefaultLauncherDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đặt làm Launcher mặc định")
                .setMessage("Bạn có muốn đặt ứng dụng SeniorCare làm màn hình chính mặc định không? Điều này sẽ giúp trải nghiệm người dùng tốt hơn.")
                .setPositiveButton("CÓ", (dialog, which) -> {
                    // Mở cài đặt để người dùng chọn launcher mặc định
                    try {
                        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Không thể mở cài đặt HOME_SETTINGS. Thử cách khác.", e);
                        Toast.makeText(this, "Không thể mở cài đặt Launcher mặc định. Vui lòng tìm 'Ứng dụng mặc định' trong cài đặt điện thoại của bạn.", Toast.LENGTH_LONG).show();
                        // Fallback: Mở cài đặt ứng dụng chung nếu không tìm thấy cài đặt Home cụ thể
                        Intent generalAppSettingsIntent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                        generalAppSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(generalAppSettingsIntent);
                        } catch (ActivityNotFoundException ex) {
                            Log.e(TAG, "Không thể mở cài đặt ứng dụng chung.", ex);
                            Toast.makeText(this, "Không thể mở cài đặt ứng dụng. Vui lòng vào Cài đặt -> Ứng dụng & thông báo -> Ứng dụng mặc định -> Ứng dụng Home để chọn SeniorCare thủ công.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("KHÔNG", (dialog, which) -> {
                    Toast.makeText(this, "Bạn có thể thay đổi trong cài đặt thiết bị bất cứ lúc nào.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                })
                .setCancelable(false) // Không cho phép đóng hộp thoại khi nhấn ra ngoài
                .show();
    }
}