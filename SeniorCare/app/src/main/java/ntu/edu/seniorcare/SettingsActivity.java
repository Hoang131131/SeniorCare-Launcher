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
    private TextView iconSizeValueTextView;
    private SeekBar textSizeSeekBar;
    private TextView textSizeValueTextView;
    private RadioGroup numColumnsRadioGroup;
    private Button manageAppsButton;
    private ImageButton contactsButton;
    private ImageButton fontSettingsButton;
    private ImageButton homeSettingsButton;

    private RecyclerView selectedAppsRecyclerView;
    private SelectedAppAdapter selectedAppAdapter;
    private List<AppInfo> selectedAppsList;

    // Define the step size and ranges for manual snapping based on your XML (min="0", max="200")
    private static final int SEEKBAR_STEP = 10; // For snapping to 10% increments
    private static final int SEEKBAR_XML_MIN = 0; // The min value in your XML
    private static final int SEEKBAR_XML_MAX = 200; // The max value in your XML

    // The actual percentage range we want to display (100% to 300%)
    // These values are taken from your SettingsUtils.java file (MIN_ICON_SIZE_PERCENTAGE and MAX_ICON_SIZE_PERCENTAGE)
    // Make sure these match the desired range for both icon and text sizes.
    private static final int ACTUAL_PERCENTAGE_MIN = 100;
    private static final int ACTUAL_PERCENTAGE_MAX_ICON = 240; // Max for icon size
    private static final int ACTUAL_PERCENTAGE_MAX_TEXT = 300; // Max for text size

    // These ranges will be used for mapping progress
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

        // KHÔNG CẦN DÒNG NÀY NỮA, vì min và max đã được đặt trong XML (0-200)
        // iconSizeSeekBar.setMax(SettingsUtils.MAX_ICON_SIZE_PERCENTAGE - SettingsUtils.MIN_ICON_SIZE_PERCENTAGE);
        // textSizeSeekBar.setMax(SettingsUtils.MAX_TEXT_SIZE_PERCENTAGE - SettingsUtils.MIN_TEXT_SIZE_PERCENTAGE);

        loadSettings(); // Gọi sau khi init seekbars
        loadSelectedApps(); // Cẩn thận: Dòng này vẫn còn nguyên từ code của bạn

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
                applyColumnLogic(finalIconSize, SettingsUtils.getTextSizePercentage(SettingsActivity.this)); // Apply logic here
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
                applyColumnLogic(SettingsUtils.getIconSizePercentage(SettingsActivity.this), finalTextSize); // Apply logic here
                sendUpdateBroadcast();
            }
        });
        // --- End: Text Size SeekBar Listener ---

        // --- Start: Column RadioGroup Listener (unchanged from your code) ---
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

        // --- Start: Button Listeners (unchanged from your code) ---
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
            Toast.makeText(this, "Điều chỉnh kích thước chữ ngay trên màn hình này.", Toast.LENGTH_SHORT).show();
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
        // --- End: Button Listeners ---
    }

    // --- Start: SeekBar utility functions (modified to be more generic) ---
    // Maps XML progress (0-200) to actual percentage (e.g., 100-240 or 100-300)
    private int mapXmlProgressToActualPercentage(int xmlProgress, int actualRange, int actualMin) {
        float scale = (float) actualRange / (SEEKBAR_XML_MAX - SEEKBAR_XML_MIN);
        return (int) (actualMin + (xmlProgress - SEEKBAR_XML_MIN) * scale);
    }

    // Maps actual percentage (e.g., 100-240 or 100-300) to XML progress (0-200)
    private int mapActualPercentageToXmlProgress(int actualPercentage, int actualRange, int actualMin) {
        float scale = (float) (SEEKBAR_XML_MAX - SEEKBAR_XML_MIN) / actualRange;
        return (int) (SEEKBAR_XML_MIN + (actualPercentage - actualMin) * scale);
    }

    // Snaps an actual percentage value to the nearest 10% step,
    // ensuring it's within the actual min/max bounds.
    private int snapActualPercentageToStep(int actualPercentage, int actualMin, int actualMax) {
        int snappedValue = Math.round((float) actualPercentage / SEEKBAR_STEP) * SEEKBAR_STEP;
        return Math.max(actualMin, Math.min(actualMax, snappedValue));
    }
    // --- End: SeekBar utility functions ---

    // --- Start: New method to apply the column logic ---
    private void applyColumnLogic(int iconSizePercentage, int textSizePercentage) {
        int currentNumColumns = SettingsUtils.getNumColumns(this); // Số cột hiện tại mà người dùng đã chọn hoặc đã được tự động điều chỉnh
        int targetNumColumns = currentNumColumns; // Khởi tạo số cột mục tiêu bằng số cột hiện tại

        // --- Bắt đầu logic điều chỉnh số cột tự động ---

        // 1. Luôn kiểm tra điều kiện cho 4 cột trước
        // Nếu đang là 4 cột VÀ kích thước icon/text vượt quá ngưỡng cho 4 cột,
        // thì không thể duy trì 4 cột được. Ta sẽ cân nhắc giảm xuống 3.
        if (targetNumColumns == 4 && (iconSizePercentage > SettingsUtils.MAX_ICON_4_COLUMNS || textSizePercentage > SettingsUtils.MAX_TEXT_4_COLUMNS)) {
            targetNumColumns = 3; // Giảm xuống 3 cột để kiểm tra tiếp
        }

        // 2. Sau đó, kiểm tra điều kiện cho 3 cột
        // Nếu hiện tại là 3 cột (hoặc đã bị giảm từ 4 xuống 3) VÀ kích thước icon/text vượt quá ngưỡng cho 3 cột,
        // thì không thể duy trì 3 cột được. Ta sẽ cân nhắc giảm xuống 2.
        if (targetNumColumns == 3 && (iconSizePercentage > SettingsUtils.MAX_ICON_3_COLUMNS || textSizePercentage > SettingsUtils.MAX_TEXT_3_COLUMNS)) {
            targetNumColumns = 2; // Giảm xuống 2 cột
        }

        // --- Kết thúc logic điều chỉnh số cột tự động ---

        // Chỉ lưu và cập nhật nếu số cột thực sự thay đổi
        if (targetNumColumns != currentNumColumns) {
            SettingsUtils.saveNumColumns(this, targetNumColumns);

            // Cập nhật hiển thị RadioButton
            if (targetNumColumns == 2) {
                numColumnsRadioGroup.check(R.id.radio_2_columns);
                Toast.makeText(this, "Kích thước icon/chữ quá lớn, tự động điều chỉnh sang 2 cột.", Toast.LENGTH_SHORT).show();
            } else if (targetNumColumns == 3) {
                numColumnsRadioGroup.check(R.id.radio_3_columns);
                Toast.makeText(this, "Kích thước icon/chữ lớn, tự động điều chỉnh sang 3 cột.", Toast.LENGTH_SHORT).show();
            } else if (targetNumColumns == 4) {
                // Trường hợp này có thể xảy ra nếu chúng ta thêm logic tăng cột,
                // nhưng hiện tại chỉ có logic giảm cột.
                numColumnsRadioGroup.check(R.id.radio_4_columns);
                Toast.makeText(this, "Số cột tự động điều chỉnh sang 4 cột.", Toast.LENGTH_SHORT).show();
            }
            sendUpdateBroadcast();
        }
        // Nếu effectiveNumColumns == currentNumColumns, tức là không có gì thay đổi,
        // thì không cần lưu lại hay gửi broadcast.
    }


    // --- Start: loadSettings (modified to use new mapping functions) ---
    private void loadSettings() {
        int iconSize = SettingsUtils.getIconSizePercentage(this);
        int mappedIconSizeProgress = mapActualPercentageToXmlProgress(iconSize, ACTUAL_PERCENTAGE_RANGE_ICON, ACTUAL_PERCENTAGE_MIN);
        iconSizeSeekBar.setProgress(mappedIconSizeProgress);
        iconSizeValueTextView.setText(String.format("%d%%", iconSize)); // Display the actual percentage

        int textSize = SettingsUtils.getTextSizePercentage(this);
        int mappedTextSizeProgress = mapActualPercentageToXmlProgress(textSize, ACTUAL_PERCENTAGE_RANGE_TEXT, ACTUAL_PERCENTAGE_MIN);
        textSizeSeekBar.setProgress(mappedTextSizeProgress);
        textSizeValueTextView.setText(String.format("%d%%", textSize)); // Display the actual percentage

        int numColumns = SettingsUtils.getNumColumns(this);
        if (numColumns == 2) {
            numColumnsRadioGroup.check(R.id.radio_2_columns);
        } else if (numColumns == 3) {
            numColumnsRadioGroup.check(R.id.radio_3_columns);
        } else if (numColumns == 4) {
            numColumnsRadioGroup.check(R.id.radio_4_columns);
        } else {
            numColumnsRadioGroup.check(R.id.radio_3_columns); // Default if saved value is invalid
            SettingsUtils.saveNumColumns(this, SettingsUtils.DEFAULT_NUM_COLUMNS);
        }
    }
    // --- End: loadSettings ---


    // --- Start: App Management Functions (UNCHANGED from your provided code) ---
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
                            } else if (savedApp.getClassName().equals(SettingsActivity.class.getName())) { // Also handle SettingsActivity
                                appIcon = getResources().getDrawable(R.drawable.ic_settings, null);
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
            // Clear and add new selection to avoid duplicates and ensure only selected apps remain
            selectedAppsList.clear();
            selectedAppsList.addAll(newlySelected);
            Collections.sort(selectedAppsList, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));
            saveSelectedApps(); // Save the updated list
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
        excludedPackages.add(getPackageName()); // Exclude self
        // Add more packages to exclude if necessary
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

            // Exclude our internal activities from the selectable list
            if (packageName.equals(getPackageName()) &&
                    (className.equals(SmsActivity.class.getName()) ||
                            className.equals(ContactsActivity.class.getName()) ||
                            className.equals(SettingsActivity.class.getName()))) {
                continue;
            }

            if (!excludedPackages.contains(packageName)) {
                String appName = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                apps.add(new AppInfo(appName, icon, packageName, className));
            }
        }

        // THÊM CÁC ỨNG DỤNG NỘI BỘ MỘT CÁCH THỦ CÔNG VÀO DANH SÁCH CÓ THỂ CHỌN NẾU CHƯA CÓ
        // (Đây là logic nếu bạn muốn chúng có thể được thêm/xóa khỏi list "selected apps"
        //  nhưng do đã thêm cứng trong MainActivity, phần này có thể không cần thiết hoặc cần điều chỉnh.)
        // Ví dụ: add SMS and Contacts if they aren't explicitly excluded by package name
        addInternalAppIfMissing(apps, "Tin nhắn", SmsActivity.class.getName(), R.drawable.ic_message);
        addInternalAppIfMissing(apps, "Danh bạ", ContactsActivity.class.getName(), R.drawable.ic_contacts);
        addInternalAppIfMissing(apps, "Cài đặt", SettingsActivity.class.getName(), R.drawable.ic_settings);


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
        // The seekbar progress and text will be updated by loadSettings() and change listeners.
        // If you want to force an update here, you can call loadSettings() again.
        loadSettings();
    }
    // --- End: App Management Functions ---
}