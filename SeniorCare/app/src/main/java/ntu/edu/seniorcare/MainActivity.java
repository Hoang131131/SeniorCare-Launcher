package ntu.edu.seniorcare;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView timeTextView;
    private TextView dateTextView;
    private TextView weatherTextView;
    private AudioManager audioManager;

    private RecyclerView appGridRecyclerView;
    private AppAdapter appAdapter;
    private List<AppInfo> appList;

    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_TICK.equals(intent.getAction()) ||
                    Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) ||
                    Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                updateDateTime();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeTextView = findViewById(R.id.time_text_view);
        dateTextView = findViewById(R.id.date_text_view);
        weatherTextView = findViewById(R.id.weather_text_view);
        ImageButton settingsButton = findViewById(R.id.settings_button);
        ImageButton volumeUpButton = findViewById(R.id.volume_up_button);
        ImageButton volumeDownButton = findViewById(R.id.volume_down_button);
        appGridRecyclerView = findViewById(R.id.app_grid_recycler_view);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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

        weatherTextView.setText("Thời tiết: Nắng"); // Placeholder

        volumeUpButton.setOnClickListener(v -> adjustVolume(true));
        volumeDownButton.setOnClickListener(v -> adjustVolume(false));

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        appList = new ArrayList<>();
        loadAndDisplayApps();
        applySettings();
    }

    private void loadAndDisplayApps() {
        appList.clear();
        PackageManager pm = getPackageManager();

        String selectedAppsJson = SettingsUtils.getSelectedAppsJson(this);

        if (selectedAppsJson != null && !selectedAppsJson.isEmpty() && !selectedAppsJson.equals("[]")) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<AppInfo>>() {}.getType();
            List<AppInfo> savedApps = gson.fromJson(selectedAppsJson, type); // savedApps giờ là List<AppInfo> mà appIcon là null

            for (AppInfo savedApp : savedApps) {
                // Tải icon cho từng savedApp
                Drawable appIcon = null;
                try {
                    // Kiểm tra xem đây có phải là ứng dụng nội bộ của chúng ta không
                    if (savedApp.getPackageName().equals(getPackageName())) {
                        if (savedApp.getClassName() != null) { // Đảm bảo className không null
                            if (savedApp.getClassName().equals(SmsActivity.class.getName())) {
                                appIcon = getResources().getDrawable(R.drawable.ic_message, null);
                            } else if (savedApp.getClassName().equals(ContactsActivity.class.getName())) {
                                appIcon = getResources().getDrawable(R.drawable.ic_contacts, null);
                            }
                        }
                    }
                    // Nếu không phải ứng dụng nội bộ hoặc chưa có icon, thử tải từ PackageManager
                    if (appIcon == null) {
                        appIcon = pm.getApplicationIcon(savedApp.getPackageName());
                    }
                    // Gán icon đã tải vào đối tượng savedApp
                    savedApp.setAppIcon(appIcon);
                    appList.add(savedApp); // Thêm đối tượng savedApp đã có icon vào appList
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("MainActivity", "App icon not found for package: " + savedApp.getPackageName() + ". Using default.", e);
                    // Gán icon mặc định nếu không tìm thấy
                    savedApp.setAppIcon(getResources().getDrawable(android.R.drawable.sym_def_app_icon, null));
                    appList.add(savedApp);
                } catch (Exception e) {
                    Log.e("MainActivity", "Error loading app icon for package: " + savedApp.getPackageName() + ". Using default.", e);
                    savedApp.setAppIcon(getResources().getDrawable(android.R.drawable.sym_def_app_icon, null));
                    appList.add(savedApp);
                }
            }
        }
        Collections.sort(appList, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));

        if (appAdapter == null) {
            appAdapter = new AppAdapter(this, appList);
            appGridRecyclerView.setAdapter(appAdapter);
        } else {
            appAdapter.setAppList(appList);
            appAdapter.notifyDataSetChanged();
        }

        applyGridLayout();
        appGridRecyclerView.setLongClickable(false);
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
        int numColumns = SettingsUtils.getNumColumns(this);

        float iconSizeFactor = (float) SettingsUtils.getIconSizePercentage(this) / 100f;
        float textSizeFactor = (float) SettingsUtils.getTextSizePercentage(this) / 100f;

        if (numColumns == 4 && (iconSizeFactor > 0.8f || textSizeFactor > 0.8f)) {
            numColumns = 3;
            Toast.makeText(this, "Kích thước icon/chữ quá lớn cho 4 cột, tự động chuyển sang 3 cột.", Toast.LENGTH_SHORT).show();
        }

        if (appGridRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) appGridRecyclerView.getLayoutManager();
            if (layoutManager.getSpanCount() != numColumns) {
                layoutManager.setSpanCount(numColumns);
                layoutManager.requestLayout();
            }
        } else {
            appGridRecyclerView.setLayoutManager(new GridLayoutManager(this, numColumns));
        }
    }

    private void updateDateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = timeFormat.format(calendar.getTime());
        timeTextView.setText(currentTime);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        String currentDate = dateFormat.format(calendar.getTime());
        dateTextView.setText(capitalizeFirstLetter(currentDate));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadAndDisplayApps();
        applySettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(timeReceiver);
        unregisterReceiver(settingsUpdateReceiver);
    }
}