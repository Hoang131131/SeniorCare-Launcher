// LauncherSettingsActivity.java
package ntu.edu.seniorcare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LauncherSettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "LauncherPrefs";
    public static final String KEY_FONT_SIZE = "font_size";
    public static final String KEY_ICON_SIZE = "icon_size";
    public static final String KEY_HIDDEN_APPS = "hidden_apps";

    public static final int FONT_SIZE_SMALL = 1;
    public static final int FONT_SIZE_MEDIUM = 2;
    public static final int FONT_SIZE_LARGE = 3;

    public static final int ICON_SIZE_SMALL = 1; // Ví dụ 48dp
    public static final int ICON_SIZE_MEDIUM = 2; // Ví dụ 64dp
    public static final int ICON_SIZE_LARGE = 3; // Ví dụ 80dp

    private SharedPreferences sharedPreferences;

    private RadioGroup radioGroupFontSize;
    private RadioGroup radioGroupIconSize;
    private RecyclerView hiddenAppsRecyclerView;
    private SettingsAppAdapter settingsAppAdapter;
    private List<AppItem> allInstalledApps; // Toàn bộ ứng dụng đã cài đặt
    private Set<String> hiddenAppPackageNames; // Package names của các ứng dụng bị ẩn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        radioGroupFontSize = findViewById(R.id.radioGroupFontSize);
        radioGroupIconSize = findViewById(R.id.radioGroupIconSize);
        hiddenAppsRecyclerView = findViewById(R.id.hiddenAppsRecyclerView);

        hiddenAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allInstalledApps = new ArrayList<>();
        hiddenAppPackageNames = sharedPreferences.getStringSet(KEY_HIDDEN_APPS, new HashSet<>());

        loadAllInstalledApps(); // Tải tất cả ứng dụng
        settingsAppAdapter = new SettingsAppAdapter(allInstalledApps, hiddenAppPackageNames, new SettingsAppAdapter.OnAppCheckChangeListener() {
            @Override
            public void onAppCheckChanged(AppItem app, boolean isChecked) {
                if (isChecked) {
                    hiddenAppPackageNames.add(app.getPackageName());
                } else {
                    hiddenAppPackageNames.remove(app.getPackageName());
                }
                saveHiddenApps();
            }
        });
        hiddenAppsRecyclerView.setAdapter(settingsAppAdapter);

        loadSettings();
        setupListeners();
    }

    private void loadAllInstalledApps() {
        PackageManager pm = getPackageManager();
        allInstalledApps.clear();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> activities = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : activities) {
            String appName = (String) info.loadLabel(pm);
            String packageName = info.activityInfo.packageName;
            Drawable appIcon = info.loadIcon(pm);

            if (!packageName.equals(getPackageName())) { // Loại trừ chính launcher
                allInstalledApps.add(new AppItem(appName, appIcon, packageName, false)); // isSystemApp tạm thời là false
            }
        }
        Collections.sort(allInstalledApps, (app1, app2) -> app1.getLabel().compareToIgnoreCase(app2.getLabel()));
    }

    private void loadSettings() {
        int fontSize = sharedPreferences.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM);
        switch (fontSize) {
            case FONT_SIZE_SMALL:
                radioGroupFontSize.check(R.id.radioSmallFont);
                break;
            case FONT_SIZE_MEDIUM:
                radioGroupFontSize.check(R.id.radioMediumFont);
                break;
            case FONT_SIZE_LARGE:
                radioGroupFontSize.check(R.id.radioLargeFont);
                break;
        }

        int iconSize = sharedPreferences.getInt(KEY_ICON_SIZE, ICON_SIZE_MEDIUM);
        switch (iconSize) {
            case ICON_SIZE_SMALL:
                radioGroupIconSize.check(R.id.radioSmallIcon);
                break;
            case ICON_SIZE_MEDIUM:
                radioGroupIconSize.check(R.id.radioMediumIcon);
                break;
            case ICON_SIZE_LARGE:
                radioGroupIconSize.check(R.id.radioLargeIcon);
                break;
        }
    }

    private void setupListeners() {
        radioGroupFontSize.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.radioSmallFont) {
                editor.putInt(KEY_FONT_SIZE, FONT_SIZE_SMALL);
            } else if (checkedId == R.id.radioMediumFont) {
                editor.putInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM);
            } else if (checkedId == R.id.radioLargeFont) {
                editor.putInt(KEY_FONT_SIZE, FONT_SIZE_LARGE);
            }
            editor.apply();
            Toast.makeText(this, "Kích thước chữ đã lưu. Khởi động lại launcher để thấy thay đổi.", Toast.LENGTH_SHORT).show();
        });

        radioGroupIconSize.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.radioSmallIcon) {
                editor.putInt(KEY_ICON_SIZE, ICON_SIZE_SMALL);
            } else if (checkedId == R.id.radioMediumIcon) {
                editor.putInt(KEY_ICON_SIZE, ICON_SIZE_MEDIUM);
            } else if (checkedId == R.id.radioLargeIcon) {
                editor.putInt(KEY_ICON_SIZE, ICON_SIZE_LARGE);
            }
            editor.apply();
            Toast.makeText(this, "Kích thước icon đã lưu. Khởi động lại launcher để thấy thay đổi.", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveHiddenApps() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(KEY_HIDDEN_APPS, hiddenAppPackageNames);
        editor.apply();
        Toast.makeText(this, "Danh sách ứng dụng ẩn đã lưu. Khởi động lại launcher để thấy thay đổi.", Toast.LENGTH_SHORT).show();
    }
}