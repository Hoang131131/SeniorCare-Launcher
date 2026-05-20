package ntu.edu.seniorcare;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // Thêm import này
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration; // Thêm import này
import android.content.res.Resources;     // Thêm import này
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;           // Thêm import này
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment; // Thêm import này
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet; // Thêm import này
import java.util.List;
import java.util.Set; // Thêm import này

public class MainActivity extends AppCompatActivity implements
        AppItemAdapter.OnAppClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener { // Implement listener này

    private ViewPager2 viewPager;
    private ImageButton btnWifi, btnVolume, btnBrightness, btnAirplaneMode;
    private AudioManager audioManager;

    private RecyclerView bottomDockRecyclerView;
    private AppItemAdapter bottomDockAdapter;
    private List<AppItem> bottomDockAppList;

    private List<AppItem> installedApps;
    private List<List<AppItem>> pagedApps;
    private YourAppPagerAdapter appPagerAdapter;

    private static final int PERMISSION_REQUEST_CODE_WRITE_SETTINGS = 1001;
    private static final int APPS_PER_PAGE = 6;

    private SharedPreferences sharedPreferences;
    private Set<String> hiddenAppPackageNames; // Package names của các ứng dụng bị ẩn


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo SharedPreferences và đăng ký listener
        sharedPreferences = getSharedPreferences(LauncherSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Load danh sách ứng dụng ẩn
        hiddenAppPackageNames = sharedPreferences.getStringSet(LauncherSettingsActivity.KEY_HIDDEN_APPS, new HashSet<>());

        // Khởi tạo các biến cho danh sách ứng dụng và phân trang
        installedApps = new ArrayList<>();
        pagedApps = new ArrayList<>();

        // Ánh xạ các ImageButton Top Bar
        btnWifi = findViewById(R.id.btn_wifi);
        btnVolume = findViewById(R.id.btn_volume);
        btnBrightness = findViewById(R.id.btn_brightness);
        btnAirplaneMode = findViewById(R.id.btn_airplane_mode);

        // Khởi tạo AudioManager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Đặt lắng nghe sự kiện click cho Top Bar
        btnWifi.setOnClickListener(v -> showWifiDialog());
        btnVolume.setOnClickListener(v -> showVolumeDialog());
        btnBrightness.setOnClickListener(v -> showBrightnessDialog());
        btnAirplaneMode.setOnClickListener(v -> toggleAirplaneMode());

        // Kiểm tra và cập nhật trạng thái icon Chế độ máy bay khi khởi tạo
        updateAirplaneModeIcon();

        // --- Cài đặt Bottom Dock ---
        bottomDockRecyclerView = findViewById(R.id.bottom_dock_recycler_view);
        bottomDockRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        bottomDockAppList = new ArrayList<>();
        bottomDockAdapter = new AppItemAdapter(bottomDockAppList, this);
        bottomDockRecyclerView.setAdapter(bottomDockAdapter);

        loadDefaultDockApps(); // Tải các ứng dụng mặc định cho Bottom Dock
        addLauncherSettingsAppToDock(); // Thêm app Cài đặt Launcher vào Dock

        // --- Cài đặt ViewPager2 (Phần giữa) ---
        viewPager = findViewById(R.id.view_pager_middle);
        loadInstalledApps(); // Tải danh sách tất cả ứng dụng (đã bao gồm lọc ẩn)
        setupAppPager();     // Thiết lập ViewPager2

        // Áp dụng các cài đặt kích thước ngay khi Activity tạo
        applyFontSizeSettings();
        applyIconSizeSettings();

        // Kiểm tra và yêu cầu đặt làm Launcher mặc định
        checkDefaultLauncher();
    }

    // --- Phương thức xử lý sự kiện click cho AppItem (dùng chung cho Bottom Dock và ViewPager) ---
    @Override
    public void onAppClick(AppItem appItem) {
        // Xử lý click vào app "Cài đặt Launcher" đặc biệt
        if (appItem.getPackageName().equals(getPackageName()) && appItem.getLabel().equals("Cài đặt Launcher")) {
            Intent intent = new Intent(this, LauncherSettingsActivity.class);
            startActivity(intent);
        } else {
            // Xử lý các app khác như bình thường
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(appItem.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không thể mở ứng dụng: " + appItem.getLabel(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi khi mở ứng dụng: " + appItem.getLabel() + ". " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("MainActivity", "Error launching app: " + appItem.getLabel(), e);
            }
        }
    }

    // --- Phương thức thêm app "Cài đặt Launcher" vào Bottom Dock ---
    private void addLauncherSettingsAppToDock() {
        AppItem settingsLauncherApp = new AppItem(
                "Cài đặt Launcher",
                ContextCompat.getDrawable(this, R.drawable.ic_settings_launcher), // Icon cho app cài đặt
                getPackageName(), // Package name của chính launcher
                true // Coi là app hệ thống để phân biệt
        );
        bottomDockAppList.add(settingsLauncherApp);
        // Không cần notifyDataSetChanged ở đây vì nó sẽ được gọi bởi loadDefaultDockApps()
    }

    // --- Phương thức tải các ứng dụng mặc định cho Bottom Dock ---
    private void loadDefaultDockApps() {
        PackageManager pm = getPackageManager();
        bottomDockAppList.clear(); // Xóa các mục cũ để tránh trùng lặp khi load lại

        // 1. Dialer (Gọi điện)
        Intent dialerIntent = new Intent(Intent.ACTION_DIAL);
        dialerIntent.setData(Uri.parse("tel:"));
        ResolveInfo dialerInfo = pm.resolveActivity(dialerIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (dialerInfo != null) {
            bottomDockAppList.add(new AppItem(
                    (String) dialerInfo.loadLabel(pm),
                    dialerInfo.loadIcon(pm),
                    dialerInfo.activityInfo.packageName,
                    true
            ));
        } else {
            Log.w("MainActivity", "Không tìm thấy ứng dụng Gọi điện mặc định.");
        }

        // 2. SMS (Nhắn tin)
        Intent smsIntent = new Intent(Intent.ACTION_MAIN);
        smsIntent.addCategory(Intent.CATEGORY_APP_MESSAGING);
        ResolveInfo smsInfo = pm.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (smsInfo != null) {
            bottomDockAppList.add(new AppItem(
                    (String) smsInfo.loadLabel(pm),
                    smsInfo.loadIcon(pm),
                    smsInfo.activityInfo.packageName,
                    true
            ));
        } else {
            Log.w("MainActivity", "Không tìm thấy ứng dụng Nhắn tin mặc định.");
        }

        // 3. Settings (Cài đặt hệ thống)
        Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
        ResolveInfo settingsInfo = pm.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (settingsInfo != null) {
            bottomDockAppList.add(new AppItem(
                    (String) settingsInfo.loadLabel(pm),
                    settingsInfo.loadIcon(pm),
                    settingsInfo.activityInfo.packageName,
                    true
            ));
        } else {
            Log.w("MainActivity", "Không tìm thấy ứng dụng Cài đặt.");
        }

        // Sau khi thêm các app mặc định, thêm app cài đặt launcher
        addLauncherSettingsAppToDock();
        bottomDockAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView của dock
    }

    // --- Phương thức tải tất cả ứng dụng đã cài đặt (cho ViewPager) ---
    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        installedApps.clear();

        // Cập nhật danh sách ứng dụng ẩn từ SharedPreferences
        hiddenAppPackageNames = sharedPreferences.getStringSet(LauncherSettingsActivity.KEY_HIDDEN_APPS, new HashSet<>());

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> activities = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : activities) {
            String appName = (String) info.loadLabel(pm);
            String packageName = info.activityInfo.packageName;
            Drawable appIcon = info.loadIcon(pm);

            // Loại trừ chính launcher của chúng ta VÀ các ứng dụng bị ẩn
            if (!packageName.equals(getPackageName()) && !hiddenAppPackageNames.contains(packageName)) {
                installedApps.add(new AppItem(appName, appIcon, packageName, false));
            }
        }

        Collections.sort(installedApps, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem app1, AppItem app2) {
                return app1.getLabel().compareToIgnoreCase(app2.getLabel());
            }
        });

        paginateApps();
    }

    // --- Phương thức phân chia danh sách ứng dụng thành các trang ---
    private void paginateApps() {
        pagedApps.clear();
        int numApps = installedApps.size();
        if (numApps == 0) return;

        int numPages = (int) Math.ceil((double) numApps / APPS_PER_PAGE);

        for (int i = 0; i < numPages; i++) {
            int startIndex = i * APPS_PER_PAGE;
            int endIndex = Math.min(startIndex + APPS_PER_PAGE, numApps);
            pagedApps.add(new ArrayList<>(installedApps.subList(startIndex, endIndex)));
        }
    }

    // --- Phương thức thiết lập ViewPager2 ---
    private void setupAppPager() {
        appPagerAdapter = new YourAppPagerAdapter(this, pagedApps, this);
        viewPager.setAdapter(appPagerAdapter);
    }

    // --- Các phương thức xử lý cài đặt Launcher mặc định ---
    private void checkDefaultLauncher() {
        String myPackageName = getPackageName();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ComponentName cn = intent.resolveActivity(pm);
        String currentLauncherPackage = cn != null ? cn.getPackageName() : "";

        if (!myPackageName.equals(currentLauncherPackage)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Đặt làm Launcher mặc định")
                    .setMessage("Để ứng dụng hoạt động tốt nhất, vui lòng đặt nó làm Launcher mặc định.")
                    .setPositiveButton("Đi đến cài đặt", (dialog, which) -> {
                        Intent setDefaultLauncherIntent = new Intent(Settings.ACTION_HOME_SETTINGS);
                        try {
                            startActivity(setDefaultLauncherIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(MainActivity.this, "Không tìm thấy cài đặt Home App.", Toast.LENGTH_SHORT).show();
                            Log.e("MainActivity", "Error opening home settings: " + e.getMessage());
                        }
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAirplaneModeIcon();

        // Tải lại cài đặt và ứng dụng khi Activity trở lại foreground
        loadInstalledApps(); // Tải lại ứng dụng (bao gồm lọc app ẩn)
        setupAppPager();     // Cài đặt lại PagerAdapter
        applyFontSizeSettings(); // Áp dụng lại kích thước chữ
        applyIconSizeSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this); // Hủy đăng ký listener
    }

    // --- Phương thức áp dụng kích thước chữ ---
    private void applyFontSizeSettings() {
        int fontSizeSetting = sharedPreferences.getInt(LauncherSettingsActivity.KEY_FONT_SIZE, LauncherSettingsActivity.FONT_SIZE_MEDIUM);
        float textSizeSp;
        switch (fontSizeSetting) {
            case LauncherSettingsActivity.FONT_SIZE_SMALL:
                textSizeSp = 14f;
                break;
            case LauncherSettingsActivity.FONT_SIZE_LARGE:
                textSizeSp = 22f;
                break;
            case LauncherSettingsActivity.FONT_SIZE_MEDIUM:
            default:
                textSizeSp = 18f;
                break;
        }

        // Áp dụng cho các TextView trong Top Bar
        TextView timeText = findViewById(R.id.text_clock_time);
        TextView dateText = findViewById(R.id.text_clock_date);
        if (timeText != null) timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp + 6); // Lớn hơn một chút cho giờ
        if (dateText != null) dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);

        // Yêu cầu Adapter của Bottom Dock và ViewPager cập nhật lại
        bottomDockAdapter.notifyDataSetChanged(); // Yêu cầu vẽ lại các item trong dock
        if (appPagerAdapter != null) {
            // Duyệt qua tất cả các Fragment hiện tại trong ViewPager
            for (int i = 0; i < appPagerAdapter.getItemCount(); i++) {
                Fragment fragment = appPagerAdapter.createFragment(i);
                if (fragment instanceof AppListFragment) {
                    ((AppListFragment) fragment).updateLayout(); // Yêu cầu fragment cập nhật layout của nó
                }
            }
        }
    }

    // --- Phương thức áp dụng kích thước icon ---
    private void applyIconSizeSettings() {
        int iconSizeSetting = sharedPreferences.getInt(LauncherSettingsActivity.KEY_ICON_SIZE, LauncherSettingsActivity.ICON_SIZE_MEDIUM);
        int iconPx; // Kích thước pixel

        Resources resources = getResources();
        switch (iconSizeSetting) {
            case LauncherSettingsActivity.ICON_SIZE_SMALL:
                iconPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, resources.getDisplayMetrics());
                break;
            case LauncherSettingsActivity.ICON_SIZE_LARGE:
                iconPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, resources.getDisplayMetrics());
                break;
            case LauncherSettingsActivity.ICON_SIZE_MEDIUM:
            default:
                iconPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, resources.getDisplayMetrics());
                break;
        }

        // Áp dụng cho các ImageButton trong Top Bar
        // Lưu ý: Cần kiểm tra null trước khi sử dụng getLayoutParams() và requestLayout()
        if (btnWifi != null) {
            btnWifi.getLayoutParams().width = iconPx;
            btnWifi.getLayoutParams().height = iconPx;
            btnWifi.requestLayout();
        }
        if (btnVolume != null) {
            btnVolume.getLayoutParams().width = iconPx;
            btnVolume.getLayoutParams().height = iconPx;
            btnVolume.requestLayout();
        }
        if (btnBrightness != null) {
            btnBrightness.getLayoutParams().width = iconPx;
            btnBrightness.getLayoutParams().height = iconPx;
            btnBrightness.requestLayout();
        }
        if (btnAirplaneMode != null) {
            btnAirplaneMode.getLayoutParams().width = iconPx;
            btnAirplaneMode.getLayoutParams().height = iconPx;
            btnAirplaneMode.requestLayout();
        }

        // Yêu cầu Adapter của Bottom Dock và ViewPager cập nhật lại
        bottomDockAdapter.notifyDataSetChanged();
        if (appPagerAdapter != null) {
            for (int i = 0; i < appPagerAdapter.getItemCount(); i++) {
                Fragment fragment = appPagerAdapter.createFragment(i);
                if (fragment instanceof AppListFragment) {
                    ((AppListFragment) fragment).updateLayout(); // Yêu cầu fragment cập nhật layout của nó
                }
            }
        }
    }

    // --- OnSharedPreferenceChangeListener ---
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("MainActivity", "Cài đặt đã thay đổi: " + key);
        // Cập nhật giao diện tùy theo cài đặt nào thay đổi
        if (key.equals(LauncherSettingsActivity.KEY_FONT_SIZE)) {
            applyFontSizeSettings();
        } else if (key.equals(LauncherSettingsActivity.KEY_ICON_SIZE)) {
            applyIconSizeSettings();
        } else if (key.equals(LauncherSettingsActivity.KEY_HIDDEN_APPS)) {
            // Khi danh sách ứng dụng ẩn thay đổi, cần tải lại và thiết lập lại danh sách ứng dụng
            loadInstalledApps(); // Tải lại ứng dụng với danh sách ẩn mới
            setupAppPager();     // Thiết lập lại PagerAdapter
            // Đồng thời, cần cập nhật lại dock nếu các app trong dock cũng bị ảnh hưởng (mặc dù thường không)
            loadDefaultDockApps();
        }
    }

    // --- Xử lý sự kiện cho các Icon cài đặt nhanh Top Bar ---
    private void showWifiDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent panelIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            try {
                startActivity(panelIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không tìm thấy cài đặt Wifi.", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Error opening Wifi settings: " + e.getMessage());
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không tìm thấy cài đặt Wifi.", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Error opening Wifi settings: " + e.getMessage());
            }
        }
    }

    private void showVolumeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_volume_brightness, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        SeekBar seekBar = dialogView.findViewById(R.id.seekbar_control);

        tvTitle.setText("Âm lượng hệ thống");

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);

        seekBar.setMax(maxVolume);
        seekBar.setProgress(currentVolume);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, progress, 0);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private void showBrightnessDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            requestWriteSettingsPermission();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_volume_brightness, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        SeekBar seekBar = dialogView.findViewById(R.id.seekbar_control);

        tvTitle.setText("Độ sáng màn hình");

        try {
            int currentBrightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (currentBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }

            int currentBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            seekBar.setMax(255);
            seekBar.setProgress(currentBrightness);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress);
                        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                        layoutParams.screenBrightness = progress / 255.0f;
                        getWindow().setAttributes(layoutParams);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        } catch (Settings.SettingNotFoundException e) {
            Log.e("MainActivity", "Lỗi đọc cài đặt độ sáng: " + e.getMessage());
            Toast.makeText(this, "Không thể truy cập cài đặt độ sáng.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private void requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PERMISSION_REQUEST_CODE_WRITE_SETTINGS);
        }
    }

    private void toggleAirplaneMode() {
        try {
            Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Vui lòng bật/tắt Chế độ máy bay trong cài đặt.", Toast.LENGTH_LONG).show();

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không tìm thấy cài đặt Chế độ máy bay.", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Error opening Airplane Mode settings: " + e.getMessage());
        }
    }

    private void updateAirplaneModeIcon() {
        boolean isAirplaneModeOn = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (isAirplaneModeOn) {
            btnAirplaneMode.setImageResource(R.drawable.ic_airplane_mode_on);
            btnAirplaneMode.setColorFilter(ContextCompat.getColor(this, android.R.color.black));
        } else {
            btnAirplaneMode.setImageResource(R.drawable.ic_airplane_mode_off);
            btnAirplaneMode.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_CODE_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    Toast.makeText(this, "Quyền ghi cài đặt đã được cấp.", Toast.LENGTH_SHORT).show();
                    showBrightnessDialog();
                } else {
                    Toast.makeText(this, "Quyền ghi cài đặt bị từ chối. Không thể thay đổi độ sáng.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}