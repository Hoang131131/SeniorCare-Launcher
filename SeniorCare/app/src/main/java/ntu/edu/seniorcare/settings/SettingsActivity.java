package ntu.edu.seniorcare.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import ntu.edu.seniorcare.apps.AppInfo;
import ntu.edu.seniorcare.contact.ContactsActivity;
import ntu.edu.seniorcare.R;
import ntu.edu.seniorcare.sms.SmsActivity;
import ntu.edu.seniorcare.apps.InstalledAppAdapter;
import ntu.edu.seniorcare.apps.SelectedAppAdapter;

public class SettingsActivity extends AppCompatActivity {

    public static final String ACTION_UPDATE_LAUNCHER = "ntu.edu.seniorcare.UPDATE_LAUNCHER";

    private SeekBar iconSizeSeekBar;
    private TextView iconSizeValueTextView;
    private SeekBar textSizeSeekBar;
    private TextView textSizeValueTextView;
    private RadioGroup numColumnsRadioGroup;
    private Button manageAppsButton;
    private ImageButton contactsButton;
    private ImageButton fontSettingsButton;
    private ImageButton homeSettingsButton; // Đổi tên từ set_default_launcher_button

    private RecyclerView selectedAppsRecyclerView;
    private SelectedAppAdapter selectedAppAdapter;
    private List<AppInfo> selectedAppsList;

    private static final int SEEKBAR_STEP = 10;
    private static final int SEEKBAR_XML_MIN = 0;
    private static final int SEEKBAR_XML_MAX = 200;

    private static final int ACTUAL_PERCENTAGE_MIN = 100;
    private static final int ACTUAL_PERCENTAGE_MAX_ICON = 240;
    private static final int ACTUAL_PERCENTAGE_MAX_TEXT = 300;

    private static final int ACTUAL_PERCENTAGE_RANGE_ICON = ACTUAL_PERCENTAGE_MAX_ICON - ACTUAL_PERCENTAGE_MIN;
    private static final int ACTUAL_PERCENTAGE_RANGE_TEXT = ACTUAL_PERCENTAGE_MAX_TEXT - ACTUAL_PERCENTAGE_MIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        iconSizeSeekBar = findViewById(R.id.icon_size_seek_bar);
        iconSizeValueTextView = findViewById(R.id.icon_size_value_text_view);
        textSizeSeekBar = findViewById(R.id.text_size_seek_bar);
        textSizeValueTextView = findViewById(R.id.text_size_value_text_view);

        numColumnsRadioGroup = findViewById(R.id.num_columns_radio_group);
        manageAppsButton = findViewById(R.id.manage_apps_button);

        contactsButton = findViewById(R.id.contacts_button);
        fontSettingsButton = findViewById(R.id.font_settings_button);
        homeSettingsButton = findViewById(R.id.set_default_launcher_button); // Id vẫn là set_default_launcher_button

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

        // --- Cập nhật icon cho các ImageButton ---
        // Icon Danh bạ
        Drawable contactsIcon = getAppIconFromIntent(new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI));
        if (contactsIcon != null) {
            contactsButton.setImageDrawable(contactsIcon);
        } else {
            // Fallback nếu không tìm thấy icon danh bạ
            contactsButton.setImageResource(R.drawable.ic_contacts); // Icon nội bộ nếu cần
        }

        // Icon Cài đặt Font/Hiển thị
        Drawable fontSettingsIcon = getAppIconFromIntent(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        if (fontSettingsIcon != null) {
            fontSettingsButton.setImageDrawable(fontSettingsIcon);
        } else {
            // Fallback nếu không tìm thấy icon cài đặt font/hiển thị
            fontSettingsButton.setImageResource(R.drawable.ic_font_settings); // Tạo icon nội bộ nếu không có
        }

        // Icon Cài đặt Home Launcher
        Drawable homeSettingsIcon = getAppIconFromIntent(new Intent(Settings.ACTION_HOME_SETTINGS));
        if (homeSettingsIcon != null) {
            homeSettingsButton.setImageDrawable(homeSettingsIcon);
        } else {
            // Fallback nếu không tìm thấy icon cài đặt home launcher
            homeSettingsButton.setImageResource(R.drawable.ic_settings); // Icon nội bộ nếu cần
        }


        // --- Start: Icon Size SeekBar Listener ---
        iconSizeSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualPercentage = mapXmlProgressToActualPercentage(progress, ACTUAL_PERCENTAGE_RANGE_ICON, ACTUAL_PERCENTAGE_MIN);
                int snappedActualPercentage = snapActualPercentageToStep(actualPercentage, ACTUAL_PERCENTAGE_MIN, ACTUAL_PERCENTAGE_MAX_ICON);

                iconSizeValueTextView.setText(String.format("%d%%", snappedActualPercentage));
                if (fromUser) {
                    int snappedXmlProgress = mapActualPercentageToXmlProgress(snappedActualPercentage, ACTUAL_PERCENTAGE_RANGE_ICON, ACTUAL_PERCENTAGE_MIN);
                    seekBar.setProgress(snappedXmlProgress);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int actualPercentage = mapXmlProgressToActualPercentage(seekBar.getProgress(), ACTUAL_PERCENTAGE_RANGE_ICON, ACTUAL_PERCENTAGE_MIN);
                int finalIconSize = snapActualPercentageToStep(actualPercentage, ACTUAL_PERCENTAGE_MIN, ACTUAL_PERCENTAGE_MAX_ICON);

                int finalXmlProgress = mapActualPercentageToXmlProgress(finalIconSize, ACTUAL_PERCENTAGE_RANGE_ICON, ACTUAL_PERCENTAGE_MIN);
                seekBar.setProgress(finalXmlProgress);

                SettingsUtils.saveIconSizePercentage(SettingsActivity.this, finalIconSize);
                applyColumnLogic(finalIconSize, SettingsUtils.getTextSizePercentage(SettingsActivity.this));
                sendUpdateBroadcast();
            }
        });
        // --- End: Icon Size SeekBar Listener ---


        // --- Start: Text Size SeekBar Listener ---
        textSizeSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualPercentage = mapXmlProgressToActualPercentage(progress, ACTUAL_PERCENTAGE_RANGE_TEXT, ACTUAL_PERCENTAGE_MIN);
                int snappedActualPercentage = snapActualPercentageToStep(actualPercentage, ACTUAL_PERCENTAGE_MIN, ACTUAL_PERCENTAGE_MAX_TEXT);

                textSizeValueTextView.setText(String.format("%d%%", snappedActualPercentage));
                if (fromUser) {
                    int snappedXmlProgress = mapActualPercentageToXmlProgress(snappedActualPercentage, ACTUAL_PERCENTAGE_RANGE_TEXT, ACTUAL_PERCENTAGE_MIN);
                    seekBar.setProgress(snappedXmlProgress);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int actualPercentage = mapXmlProgressToActualPercentage(seekBar.getProgress(), ACTUAL_PERCENTAGE_RANGE_TEXT, ACTUAL_PERCENTAGE_MIN);
                int finalTextSize = snapActualPercentageToStep(actualPercentage, ACTUAL_PERCENTAGE_MIN, ACTUAL_PERCENTAGE_MAX_TEXT);

                int finalXmlProgress = mapActualPercentageToXmlProgress(finalTextSize, ACTUAL_PERCENTAGE_RANGE_TEXT, ACTUAL_PERCENTAGE_MIN);
                seekBar.setProgress(finalXmlProgress);

                SettingsUtils.saveTextSizePercentage(SettingsActivity.this, finalTextSize);
                applyColumnLogic(SettingsUtils.getIconSizePercentage(SettingsActivity.this), finalTextSize);
                sendUpdateBroadcast();
            }
        });
        // --- End: Text Size SeekBar Listener ---

        // --- Start: Column RadioGroup Listener ---
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
        // --- End: Column RadioGroup Listener ---

        // --- Start: Button Listeners (Đã sửa) ---
        manageAppsButton.setOnClickListener(v -> showAppSelectionDialog());

        // Nút Danh bạ
        contactsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không tìm thấy ứng dụng Danh bạ mặc định.", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Cài đặt Font/Hiển thị
        fontSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // Fallback cho một số thiết bị không có ACTION_DISPLAY_SETTINGS
                Toast.makeText(this, "Không thể mở cài đặt hiển thị trực tiếp. Mở cài đặt chung.", Toast.LENGTH_SHORT).show();
                Intent generalSettingsIntent = new Intent(Settings.ACTION_SETTINGS);
                try {
                    startActivity(generalSettingsIntent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(this, "Không thể mở cài đặt.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Nút Cài đặt Launcher mặc định
        homeSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Không thể mở cài đặt Launcher mặc định. Vui lòng tìm 'Ứng dụng mặc định' trong cài đặt điện thoại của bạn.", Toast.LENGTH_LONG).show();
                Intent generalAppSettingsIntent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                try {
                    startActivity(generalAppSettingsIntent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(this, "Không thể mở cài đặt ứng dụng. Vui lòng tìm thủ công.", Toast.LENGTH_LONG).show();
                }
            }
        });
        // --- End: Button Listeners ---
    }

    // --- Start: NEW helper method to get app icon from an Intent ---
    private Drawable getAppIconFromIntent(Intent intent) {
        PackageManager pm = getPackageManager();
        // Cần FLAG_MATCH_DEFAULT_ONLY để tìm ứng dụng mặc định cho intent này
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (!list.isEmpty()) {
            // Lấy ứng dụng đầu tiên trong danh sách (thường là ứng dụng mặc định)
            return list.get(0).loadIcon(pm);
        }
        return null;
    }
    // --- End: NEW helper method ---

    private int mapXmlProgressToActualPercentage(int xmlProgress, int actualRange, int actualMin) {
        float scale = (float) actualRange / (SEEKBAR_XML_MAX - SEEKBAR_XML_MIN);
        return (int) (actualMin + (xmlProgress - SEEKBAR_XML_MIN) * scale);
    }

    private int mapActualPercentageToXmlProgress(int actualPercentage, int actualRange, int actualMin) {
        float scale = (float) (SEEKBAR_XML_MAX - SEEKBAR_XML_MIN) / actualRange;
        return (int) (SEEKBAR_XML_MIN + (actualPercentage - actualMin) * scale);
    }

    private int snapActualPercentageToStep(int actualPercentage, int actualMin, int actualMax) {
        int snappedValue = Math.round((float) actualPercentage / SEEKBAR_STEP) * SEEKBAR_STEP;
        return Math.max(actualMin, Math.min(actualMax, snappedValue));
    }

    private void applyColumnLogic(int iconSizePercentage, int textSizePercentage) {
        int currentNumColumns = SettingsUtils.getNumColumns(this);
        int targetNumColumns = currentNumColumns;

        if (targetNumColumns == 4 && (iconSizePercentage > SettingsUtils.MAX_ICON_4_COLUMNS || textSizePercentage > SettingsUtils.MAX_TEXT_4_COLUMNS)) {
            targetNumColumns = 3;
        }

        if (targetNumColumns == 3 && (iconSizePercentage > SettingsUtils.MAX_ICON_3_COLUMNS || textSizePercentage > SettingsUtils.MAX_TEXT_3_COLUMNS)) {
            targetNumColumns = 2;
        }

        if (targetNumColumns != currentNumColumns) {
            SettingsUtils.saveNumColumns(this, targetNumColumns);

            if (targetNumColumns == 2) {
                numColumnsRadioGroup.check(R.id.radio_2_columns);
                Toast.makeText(this, "Kích thước icon/chữ quá lớn, tự động điều chỉnh sang 2 cột.", Toast.LENGTH_SHORT).show();
            } else if (targetNumColumns == 3) {
                numColumnsRadioGroup.check(R.id.radio_3_columns);
                Toast.makeText(this, "Kích thước icon/chữ lớn, tự động điều chỉnh sang 3 cột.", Toast.LENGTH_SHORT).show();
            } else if (targetNumColumns == 4) {
                numColumnsRadioGroup.check(R.id.radio_4_columns);
                Toast.makeText(this, "Số cột tự động điều chỉnh sang 4 cột.", Toast.LENGTH_SHORT).show();
            }
            sendUpdateBroadcast();
        }
    }

    private void loadSettings() {
        int iconSize = SettingsUtils.getIconSizePercentage(this);
        int mappedIconSizeProgress = mapActualPercentageToXmlProgress(iconSize, ACTUAL_PERCENTAGE_RANGE_ICON, ACTUAL_PERCENTAGE_MIN);
        iconSizeSeekBar.setProgress(mappedIconSizeProgress);
        iconSizeValueTextView.setText(String.format("%d%%", iconSize));

        int textSize = SettingsUtils.getTextSizePercentage(this);
        int mappedTextSizeProgress = mapActualPercentageToXmlProgress(textSize, ACTUAL_PERCENTAGE_RANGE_TEXT, ACTUAL_PERCENTAGE_MIN);
        textSizeSeekBar.setProgress(mappedTextSizeProgress);
        textSizeValueTextView.setText(String.format("%d%%", textSize));

        int numColumns = SettingsUtils.getNumColumns(this);
        if (numColumns == 2) {
            numColumnsRadioGroup.check(R.id.radio_2_columns);
        } else if (numColumns == 3) {
            numColumnsRadioGroup.check(R.id.radio_3_columns);
        } else if (numColumns == 4) {
            numColumnsRadioGroup.check(R.id.radio_4_columns);
        } else {
            numColumnsRadioGroup.check(R.id.radio_3_columns);
            SettingsUtils.saveNumColumns(this, SettingsUtils.DEFAULT_NUM_COLUMNS);
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
                    if (savedApp.getPackageName().equals(getPackageName())) {
                        if (savedApp.getClassName() != null) {
                            if (savedApp.getClassName().equals(SmsActivity.class.getName())) {
                                appIcon = getResources().getDrawable(R.drawable.ic_message, null);
                            } else if (savedApp.getClassName().equals(ContactsActivity.class.getName())) {
                                appIcon = getResources().getDrawable(R.drawable.ic_contacts, null);
                            }
                        }
                    }
                    if (appIcon == null) {
                        appIcon = pm.getApplicationIcon(savedApp.getPackageName());
                    }
                    savedApp.setAppIcon(appIcon);
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

            if (packageName.equals(getPackageName()) &&
                    (className.equals(SmsActivity.class.getName()) ||
                            className.equals(ContactsActivity.class.getName()))) {
                continue;
            }

            if (!excludedPackages.contains(packageName)) {
                String appName = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                apps.add(new AppInfo(appName, icon, packageName, className));
            }
        }

        addInternalAppIfMissing(apps, "Tin nhắn", SmsActivity.class.getName(), R.drawable.ic_message);
        addInternalAppIfMissing(apps, "Danh bạ", ContactsActivity.class.getName(), R.drawable.ic_contacts);


        Collections.sort(apps, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));
        return apps;
    }

    private void addInternalAppIfMissing(List<AppInfo> apps, String appName, String className, int drawableResId) {
        boolean found = false;
        for (AppInfo app : apps) {
            if (app.getPackageName().equals(getPackageName()) && app.getClassName().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            Drawable appIcon = null;
            try {
                appIcon = getResources().getDrawable(drawableResId, null);
            } catch (android.content.res.Resources.NotFoundException e) {
                Log.e("SettingsActivity", drawableResId + " drawable not found", e);
                appIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
            }
            apps.add(new AppInfo(appName, appIcon, getPackageName(), className));
        }
    }


    private void sendUpdateBroadcast() {
        Intent intent = new Intent(ACTION_UPDATE_LAUNCHER);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSelectedApps();
        loadSettings();
    }

    // --- SimpleSeekBarChangeListener (UNCHANGED) ---
    private abstract class SimpleSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // Implemented by subclasses
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Not used
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Implemented by subclasses
        }
    }
}