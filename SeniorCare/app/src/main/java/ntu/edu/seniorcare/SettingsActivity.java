package ntu.edu.seniorcare;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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
                ((TextView)findViewById(R.id.icon_size_value_text_view)).setText(progress + "%");
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
                ((TextView)findViewById(R.id.text_size_value_text_view)).setText(progress + "%");
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
            Intent intent = new Intent(this, ContactsActivity.class);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không thể mở ứng dụng danh bạ nội bộ.", Toast.LENGTH_SHORT).show();
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
        ((TextView)findViewById(R.id.icon_size_value_text_view)).setText(SettingsUtils.getIconSizePercentage(this) + "%");
        iconSizeSeekBar.setProgress(SettingsUtils.getIconSizePercentage(this));

        ((TextView)findViewById(R.id.text_size_value_text_view)).setText(SettingsUtils.getTextSizePercentage(this) + "%");
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
                    savedApp.setAppIcon(appIcon); // Gán icon đã tải vào đối tượng savedApp
                    tempLoadedApps.add(savedApp);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("SettingsActivity", "App icon not found for package: " + savedApp.getPackageName() + ". Using default.", e);
                    savedApp.setAppIcon(getResources().getDrawable(android.R.drawable.sym_def_app_icon, null));
                    tempLoadedApps.add(savedApp);
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Error loading app icon for package: " + savedApp.getPackageName() + ". Using default.", e);
                    savedApp.setAppIcon(getResources().getDrawable(android.R.drawable.sym_def_app_icon, null));
                    tempLoadedApps.add(savedApp);
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
        // Gson sẽ tự động bỏ qua trường transient Drawable appIcon
        String json = gson.toJson(selectedAppsList);
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
        Set<String> currentlySelectedAppIdentifiers = new HashSet<>();
        for (AppInfo app : selectedAppsList) {
            currentlySelectedAppIdentifiers.add(app.getPackageName() + "/" + app.getClassName());
        }

        InstalledAppAdapter installedAppAdapter = new InstalledAppAdapter(this, allInstalledApps, currentlySelectedAppIdentifiers);
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

        Set<String> excludedPackages = new HashSet<>();
        excludedPackages.add(getPackageName());
        excludedPackages.add("com.android.stk");
        excludedPackages.add("com.samsung.android.app.simcardmanager");
        excludedPackages.add("com.qualcomm.qti.simcontacts");
        excludedPackages.add("com.android.settings");
        excludedPackages.add("com.google.android.inputmethod.latin");
        excludedPackages.add("com.google.android.gms");
        excludedPackages.add("com.android.systemui");
        excludedPackages.add("com.google.android.packageinstaller");
        excludedPackages.add("com.android.providers.media");
        excludedPackages.add("com.android.providers.downloads");
        excludedPackages.add("com.android.documentsui");
        excludedPackages.add("com.android.launcher3");
        excludedPackages.add("com.google.android.apps.nexuslauncher");
        excludedPackages.add("com.samsung.android.app.homelauncher");
        excludedPackages.add("com.miui.home");
        excludedPackages.add("com.huawei.android.launcher");
        excludedPackages.add("com.oneplus.hydrogen.launcher");
        excludedPackages.add("com.oppo.launcher");
        excludedPackages.add("com.vivo.launcher");
        excludedPackages.add("android");

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo ri : resolveInfoList) {
            String packageName = ri.activityInfo.packageName;
            String className = ri.activityInfo.name;

            if (!excludedPackages.contains(packageName)) {
                String appName = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                apps.add(new AppInfo(appName, icon, packageName, className));
            }
        }

        // THÊM CÁC ỨNG DỤNG NỘI BỘ MỘT CÁCH THỦ CÔNG
        String smsAppName = "Tin nhắn";
        String smsPackageName = getPackageName();
        String smsClassName = SmsActivity.class.getName();
        Drawable smsAppIcon = null;
        try {
            smsAppIcon = getResources().getDrawable(R.drawable.ic_message, null);
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e("SettingsActivity", "ic_message drawable not found", e);
            smsAppIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
        }
        apps.add(new AppInfo(smsAppName, smsAppIcon, smsPackageName, smsClassName));

        String contactsAppName = "Danh bạ";
        String contactsPackageName = getPackageName();
        String contactsClassName = ContactsActivity.class.getName();
        Drawable contactsAppIcon = null;
        try {
            contactsAppIcon = getResources().getDrawable(R.drawable.ic_contacts, null);
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e("SettingsActivity", "ic_contacts drawable not found", e);
            contactsAppIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
        }
        apps.add(new AppInfo(contactsAppName, contactsAppIcon, contactsPackageName, contactsClassName));

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