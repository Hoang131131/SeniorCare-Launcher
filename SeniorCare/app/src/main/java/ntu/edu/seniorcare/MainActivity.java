package ntu.edu.seniorcare;


import static ntu.edu.seniorcare.SettingsUtils.MAX_ICON_3_COLUMNS;
import static ntu.edu.seniorcare.SettingsUtils.MAX_ICON_4_COLUMNS;
import static ntu.edu.seniorcare.SettingsUtils.MAX_TEXT_3_COLUMNS;
import static ntu.edu.seniorcare.SettingsUtils.MAX_TEXT_4_COLUMNS;

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
import java.util.Objects; // Import Objects để dùng Objects.equals

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
                loadAndDisplayApps(); // Cần load lại app để cập nhật danh sách đã chọn và áp dụng icon mới
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

        appList = new ArrayList<>();
        loadAndDisplayApps();
        applySettings();
    }

    private void loadAndDisplayApps() {
        appList.clear();
        PackageManager pm = getPackageManager();

        // -----------------------------------------------------------------
        // Thêm SettingsActivity, SmsActivity, ContactsActivity vào danh sách ứng dụng cố định
        // Đây là cách bạn đảm bảo chúng luôn hiển thị và không bị xóa khỏi danh sách selectedApps
        // và cũng giúp tránh phải chọn lại từ danh sách ứng dụng đã cài đặt.

        // SettingsActivity
        AppInfo settingsApp = new AppInfo(
                "Cài đặt",
                getResources().getDrawable(R.drawable.ic_settings, null),
                getPackageName(),
                SettingsActivity.class.getName()
        );
        appList.add(settingsApp);

        // SmsActivity
        AppInfo smsApp = new AppInfo(
                "Tin nhắn",
                getResources().getDrawable(R.drawable.ic_message, null),
                getPackageName(),
                SmsActivity.class.getName()
        );
        appList.add(smsApp);

        // ContactsActivity
        AppInfo contactsApp = new AppInfo(
                "Danh bạ",
                getResources().getDrawable(R.drawable.ic_contacts, null),
                getPackageName(),
                ContactsActivity.class.getName()
        );
        appList.add(contactsApp);

        // -----------------------------------------------------------------

        String selectedAppsJson = SettingsUtils.getSelectedAppsJson(this);

        if (selectedAppsJson != null && !selectedAppsJson.isEmpty() && !selectedAppsJson.equals("[]")) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<AppInfo>>() {}.getType();
            List<AppInfo> savedApps = gson.fromJson(selectedAppsJson, type);

            if (savedApps != null) {
                for (AppInfo savedApp : savedApps) {
                    // Kiểm tra nếu ứng dụng đã tồn tại (là các ứng dụng cố định của chúng ta) thì bỏ qua
                    // Sử dụng Objects.equals cho className để tránh NullPointerException
                    if (getPackageName().equals(savedApp.getPackageName()) &&
                            (Objects.equals(savedApp.getClassName(), SettingsActivity.class.getName()) ||
                                    Objects.equals(savedApp.getClassName(), SmsActivity.class.getName()) ||
                                    Objects.equals(savedApp.getClassName(), ContactsActivity.class.getName()))) {
                        continue;
                    }

                    Drawable appIcon = null;
                    try {
                        appIcon = pm.getApplicationIcon(savedApp.getPackageName());
                        savedApp.setAppIcon(appIcon);
                        appList.add(savedApp);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("MainActivity", "App icon not found for package: " + savedApp.getPackageName() + ". Using default.", e);
                        savedApp.setAppIcon(getResources().getDrawable(android.R.drawable.sym_def_app_icon, null));
                        appList.add(savedApp);
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error loading app icon for package: " + savedApp.getPackageName() + ". Using default.", e);
                        savedApp.setAppIcon(getResources().getDrawable(android.R.drawable.sym_def_app_icon, null));
                        appList.add(savedApp);
                    }
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
    }

    private void applySettings() {
        // Factors are for the adapter to scale drawing
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