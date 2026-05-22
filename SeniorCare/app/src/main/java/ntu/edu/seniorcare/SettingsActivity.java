package ntu.edu.seniorcare;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    public static final String ACTION_UPDATE_LAUNCHER = "ntu.edu.seniorcare.UPDATE_LAUNCHER";

    private SeekBar iconSizeSeekBar;
    private SeekBar textSizeSeekBar;
    private RadioGroup numColumnsRadioGroup;
    private Button manageAppsButton;
    private ImageButton contactsButton;
    private ImageButton fontSettingsButton;
    private ImageButton homeSettingsButton;

    private RecyclerView selectedAppsRecyclerView;
    private SelectedAppAdapter selectedAppAdapter;
    private List<AppInfo> selectedAppsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        iconSizeSeekBar = findViewById(R.id.icon_size_seek_bar);
        textSizeSeekBar = findViewById(R.id.text_size_seek_bar);
        numColumnsRadioGroup = findViewById(R.id.num_columns_radio_group);
        manageAppsButton = findViewById(R.id.manage_apps_button);

        contactsButton = findViewById(R.id.contacts_button);
        fontSettingsButton = findViewById(R.id.font_settings_button);
        homeSettingsButton = findViewById(R.id.set_default_launcher_button);

        selectedAppsRecyclerView = findViewById(R.id.selected_apps_recycler_view);
        selectedAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        selectedAppsList = new ArrayList<>();
        selectedAppAdapter = new SelectedAppAdapter(this, selectedAppsList, new SelectedAppAdapter.OnAppRemovedListener() {
            @Override
            public void onAppRemoved(AppInfo app) {
                removeAppFromSelectedList(app);
            }
        });
        selectedAppsRecyclerView.setAdapter(selectedAppAdapter);

        loadSettings();
        loadSelectedApps();

        iconSizeSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Do nothing here, save onStopTrackingTouch
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SettingsUtils.saveIconSizePercentage(SettingsActivity.this, seekBar.getProgress());
                sendUpdateBroadcast();
            }
        });

        textSizeSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Do nothing here, save onStopTrackingTouch
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SettingsUtils.saveTextSizePercentage(SettingsActivity.this, seekBar.getProgress());
                sendUpdateBroadcast();
            }
        });

        numColumnsRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int numColumns = 0;
            if (checkedId == R.id.radio_2_columns) {
                numColumns = 2;
            } else if (checkedId == R.id.radio_3_columns) {
                numColumns = 3;
            } else if (checkedId == R.id.radio_4_columns) {
                numColumns = 4;
            }
            SettingsUtils.saveNumColumns(SettingsActivity.this, numColumns);
            sendUpdateBroadcast();
        });

        manageAppsButton.setOnClickListener(v -> showAppSelectionDialog());

        contactsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không thể mở ứng dụng danh bạ.", Toast.LENGTH_SHORT).show();
            }
        });

        fontSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không thể mở cài đặt hiển thị (cỡ chữ).", Toast.LENGTH_SHORT).show();
            }
        });

        homeSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không thể mở cài đặt Launcher mặc định. Vui lòng tìm 'Ứng dụng mặc định' trong cài đặt điện thoại của bạn.", Toast.LENGTH_LONG).show();
                Intent generalSettingsIntent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                try {
                    startActivity(generalSettingsIntent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(this, "Không thể mở cài đặt ứng dụng. Vui lòng tìm thủ công.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadSettings() {
        iconSizeSeekBar.setProgress(SettingsUtils.getIconSizePercentage(this));
        textSizeSeekBar.setProgress(SettingsUtils.getTextSizePercentage(this));

        int numColumns = SettingsUtils.getNumColumns(this);
        if (numColumns == 2) {
            numColumnsRadioGroup.check(R.id.radio_2_columns);
        } else if (numColumns == 3) {
            numColumnsRadioGroup.check(R.id.radio_3_columns);
        } else if (numColumns == 4) {
            numColumnsRadioGroup.check(R.id.radio_4_columns);
        } else {
            numColumnsRadioGroup.check(R.id.radio_3_columns);
            SettingsUtils.saveNumColumns(this, 3);
        }
    }

    private void loadSelectedApps() {
        String json = SettingsUtils.getSelectedAppsJson(this);
        List<AppInfo> tempLoadedApps = new ArrayList<>();

        if (json != null && !json.isEmpty() && !json.equals("[]")) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<AppInfo>>() {}.getType();
            List<AppInfo> savedApps = gson.fromJson(json, type);

            PackageManager pm = getPackageManager();
            for (AppInfo savedApp : savedApps) {
                try {
                    Intent launchIntent = pm.getLaunchIntentForPackage(savedApp.getPackageName());
                    if (launchIntent != null) {
                        ResolveInfo ri = pm.resolveActivity(launchIntent, 0);
                        if (ri != null) {
                            tempLoadedApps.add(new AppInfo(ri.loadLabel(pm).toString(), ri.loadIcon(pm), ri.activityInfo.packageName));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        selectedAppsList.clear();
        selectedAppsList.addAll(tempLoadedApps);
        Collections.sort(selectedAppsList, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));
        selectedAppAdapter.notifyDataSetChanged();
    }

    private void saveSelectedApps() {
        Gson gson = new Gson();
        List<AppInfo> appsToSave = new ArrayList<>();
        for (AppInfo app : selectedAppsList) {
            appsToSave.add(new AppInfo(app.getAppName(), null, app.getPackageName()));
        }
        String json = gson.toJson(appsToSave);
        SettingsUtils.saveSelectedAppsJson(this, json);
        sendUpdateBroadcast();
    }

    private void removeAppFromSelectedList(AppInfo app) {
        selectedAppsList.remove(app);
        saveSelectedApps();
        selectedAppAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Đã xóa " + app.getAppName() + " khỏi Launcher.", Toast.LENGTH_SHORT).show();
    }

    private void showAppSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn ứng dụng để hiển thị trên Launcher");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_selection, null);
        RecyclerView allAppsRecyclerView = dialogView.findViewById(R.id.all_apps_recycler_view);
        allAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        builder.setView(dialogView);

        List<AppInfo> allInstalledApps = loadAllApps();
        Set<String> currentlySelectedPackages = new HashSet<>();
        for (AppInfo app : selectedAppsList) {
            currentlySelectedPackages.add(app.getPackageName());
        }

        InstalledAppAdapter installedAppAdapter = new InstalledAppAdapter(this, allInstalledApps, currentlySelectedPackages);
        allAppsRecyclerView.setAdapter(installedAppAdapter);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            List<AppInfo> newlySelected = installedAppAdapter.getSelectedApps();
            selectedAppsList.clear();
            selectedAppsList.addAll(newlySelected);
            Collections.sort(selectedAppsList, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));
            saveSelectedApps();
            selectedAppAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Đã cập nhật ứng dụng trên Launcher.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private List<AppInfo> loadAllApps() {
        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = getPackageManager();

        // Danh sách các package CẦN loại trừ
        Set<String> excludedPackages = new HashSet<>();
        excludedPackages.add(getPackageName()); // Loại trừ chính Launcher của chúng ta
        // Các gói hệ thống và tiện ích mà người dùng thường không muốn hiển thị trên Launcher
        excludedPackages.add("com.android.stk"); // SIM Toolkit
        excludedPackages.add("com.samsung.android.app.simcardmanager"); // Samsung SIM Manager
        excludedPackages.add("com.qualcomm.qti.simcontacts"); // Qualcomm SIM Contacts
        excludedPackages.add("com.android.settings"); // Ứng dụng Cài đặt (người dùng có thể mở bằng nút chuyên dụng)
        excludedPackages.add("com.google.android.inputmethod.latin"); // Bàn phím
        excludedPackages.add("com.google.android.gms"); // Dịch vụ Google Play (không phải ứng dụng để khởi chạy)
        excludedPackages.add("com.android.systemui"); // Giao diện người dùng hệ thống
        excludedPackages.add("com.google.android.packageinstaller"); // Trình cài đặt gói
        excludedPackages.add("com.android.providers.media"); // Bộ lưu trữ phương tiện
        excludedPackages.add("com.android.providers.downloads"); // Trình quản lý tải xuống
        excludedPackages.add("com.android.documentsui"); // Ứng dụng Tệp
        // Các Launcher mặc định khác (để tránh xung đột hoặc hiển thị bản thân chúng)
        excludedPackages.add("com.android.launcher3");
        excludedPackages.add("com.google.android.apps.nexuslauncher");
        excludedPackages.add("com.samsung.android.app.homelauncher");
        excludedPackages.add("com.miui.home");
        excludedPackages.add("com.huawei.android.launcher");
        excludedPackages.add("com.oneplus.hydrogen.launcher");
        excludedPackages.add("com.oppo.launcher");
        excludedPackages.add("com.vivo.launcher");
        excludedPackages.add("android"); // Tiến trình hệ thống Android

        // Intent để truy vấn các ứng dụng có thể khởi chạy
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo ri : resolveInfoList) {
            String packageName = ri.activityInfo.packageName;

            // Chỉ thêm ứng dụng nếu nó KHÔNG nằm trong danh sách loại trừ
            if (!excludedPackages.contains(packageName)) {
                String appName = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                apps.add(new AppInfo(appName, icon, packageName));
            }
        }

        // Sắp xếp danh sách theo tên ứng dụng
        Collections.sort(apps, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));
        return apps;
    }


    private void sendUpdateBroadcast() {
        Intent intent = new Intent(ACTION_UPDATE_LAUNCHER);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSelectedApps();
    }
}