package ntu.edu.seniorcare;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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

    private SeekBar iconSizeSeekBar;
    private SeekBar textSizeSeekBar;
    private RadioGroup columnRadioGroup;
    private Button addAppsButton;
    private RecyclerView selectedAppsRecyclerView;
    private SelectedAppAdapter selectedAppAdapter;
    private List<AppInfo> selectedAppsList; // Apps already in launcher
    private Set<String> selectedAppPackageNames; // For quick lookup

    private TextView currentIconSizeTv;
    private TextView currentTextSizeTv;

    // Define the custom broadcast action string here
    public static final String ACTION_UPDATE_LAUNCHER = "ntu.edu.seniorcare.UPDATE_LAUNCHER"; // <--- Sửa ở đây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        iconSizeSeekBar = findViewById(R.id.icon_size_seek_bar);
        textSizeSeekBar = findViewById(R.id.text_size_seek_bar);
        columnRadioGroup = findViewById(R.id.column_radio_group);
        addAppsButton = findViewById(R.id.add_apps_button);
        selectedAppsRecyclerView = findViewById(R.id.selected_apps_recycler_view);
        currentIconSizeTv = findViewById(R.id.current_icon_size_tv);
        currentTextSizeTv = findViewById(R.id.current_text_size_tv);
        Button openContactsButton = findViewById(R.id.open_contacts_button);
        Button openFontSettingsButton = findViewById(R.id.open_font_settings_button);
        Button setDefaultLauncherButton = findViewById(R.id.set_default_launcher_button);


        loadSelectedApps(); // Load previously selected apps

        // Setup Selected Apps RecyclerView
        selectedAppAdapter = new SelectedAppAdapter(this, selectedAppsList, new SelectedAppAdapter.OnAppLongClickListener() {
            @Override
            public void onAppLongClick(AppInfo app) {
                showRemoveAppDialog(app);
            }
        });
        selectedAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedAppsRecyclerView.setAdapter(selectedAppAdapter);


        // --- Icon and Text Size SeekBars ---
        iconSizeSeekBar.setMax(100); // Scale from 0 to 100 for percentage
        textSizeSeekBar.setMax(100);

        int savedIconSize = SettingsUtils.getIconSizePercentage(this);
        int savedTextSize = SettingsUtils.getTextSizePercentage(this);
        iconSizeSeekBar.setProgress(savedIconSize);
        textSizeSeekBar.setProgress(savedTextSize);
        currentIconSizeTv.setText(savedIconSize + "%");
        currentTextSizeTv.setText(savedTextSize + "%");


        iconSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentIconSizeTv.setText(progress + "%");
                SettingsUtils.saveIconSizePercentage(SettingsActivity.this, progress);
                sendBroadcast(new Intent(ACTION_UPDATE_LAUNCHER)); // <--- Sửa ở đây
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentTextSizeTv.setText(progress + "%");
                SettingsUtils.saveTextSizePercentage(SettingsActivity.this, progress);
                sendBroadcast(new Intent(ACTION_UPDATE_LAUNCHER)); // <--- Sửa ở đây
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // --- Column RadioGroup ---
        int savedColumns = SettingsUtils.getNumColumns(this);
        if (savedColumns == 3) {
            columnRadioGroup.check(R.id.radio_3_columns);
        } else {
            columnRadioGroup.check(R.id.radio_4_columns);
        }

        columnRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int columns = (checkedId == R.id.radio_3_columns) ? 3 : 4;
            SettingsUtils.saveNumColumns(SettingsActivity.this, columns);
            sendBroadcast(new Intent(ACTION_UPDATE_LAUNCHER)); // <--- Sửa ở đây
        });

        // --- Add Apps Button ---
        addAppsButton.setOnClickListener(v -> showAddAppsDialog());

        // --- Open Contacts Button ---
        openContactsButton.setOnClickListener(v -> openSystemContacts());

        // --- Open Font Settings Button ---
        openFontSettingsButton.setOnClickListener(v -> openSystemFontSettings());

        // --- Set Default Launcher Button ---
        setDefaultLauncherButton.setOnClickListener(v -> openDefaultLauncherSettings());
    }

    private void loadSelectedApps() {
        String json = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
                .getString("selected_apps", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<AppInfo>>() {}.getType();
            selectedAppsList = gson.fromJson(json, type);
        } else {
            selectedAppsList = new ArrayList<>();
        }
        selectedAppPackageNames = new HashSet<>();
        for (AppInfo app : selectedAppsList) {
            selectedAppPackageNames.add(app.getPackageName());
        }
    }

    private void saveSelectedApps() {
        Gson gson = new Gson();
        String json = gson.toJson(selectedAppsList);
        getSharedPreferences("launcher_prefs", MODE_PRIVATE)
                .edit()
                .putString("selected_apps", json)
                .apply();
        sendBroadcast(new Intent(ACTION_UPDATE_LAUNCHER)); // <--- Sửa ở đây
    }

    private void showAddAppsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn ứng dụng để thêm");

        List<AppInfo> installedApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> availableApps = pm.queryIntentActivities(i, 0);

        for (ResolveInfo ri : availableApps) {
            // Only add apps not already selected
            if (!selectedAppPackageNames.contains(ri.activityInfo.packageName)) {
                AppInfo app = new AppInfo(ri.loadLabel(pm).toString(), ri.loadIcon(pm), ri.activityInfo.packageName);
                installedApps.add(app);
            }
        }

        // Sort installed apps alphabetically
        Collections.sort(installedApps, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));

        // Use a custom layout for the dialog to include CheckBoxes
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_apps, null);
        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.dialog_apps_recycler_view);
        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        InstalledAppAdapter dialogAdapter = new InstalledAppAdapter(this, installedApps);
        dialogRecyclerView.setAdapter(dialogAdapter);

        builder.setView(dialogView);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            List<AppInfo> appsToAdd = dialogAdapter.getSelectedApps();
            for (AppInfo app : appsToAdd) {
                selectedAppsList.add(app);
                selectedAppPackageNames.add(app.getPackageName());
            }
            Collections.sort(selectedAppsList, (app1, app2) -> app1.getAppName().compareToIgnoreCase(app2.getAppName()));
            selectedAppAdapter.notifyDataSetChanged();
            saveSelectedApps();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showRemoveAppDialog(AppInfo app) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa ứng dụng")
                .setMessage("Bạn có muốn xóa '" + app.getAppName() + "' khỏi Launcher không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    selectedAppsList.remove(app);
                    selectedAppPackageNames.remove(app.getPackageName());
                    selectedAppAdapter.notifyDataSetChanged();
                    saveSelectedApps();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openSystemContacts() {
        Intent intent = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Không thể mở ứng dụng Danh bạ.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSystemFontSettings() {
        Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
        try {
            startActivity(intent);
            Toast.makeText(this, "Tìm mục 'Kích thước hiển thị và văn bản' hoặc tương tự.", Toast.LENGTH_LONG).show();
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Không thể mở cài đặt hiển thị. Vui lòng tìm thủ công trong cài đặt hệ thống.", Toast.LENGTH_LONG).show();
        }
    }

    private void openDefaultLauncherSettings() {
        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Không thể mở cài đặt Launcher trực tiếp. Vui lòng tìm 'Ứng dụng mặc định' trong cài đặt điện thoại của bạn.", Toast.LENGTH_LONG).show();
            // Fallback to general app settings, from where user might find default apps
            Intent generalSettingsIntent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            try {
                startActivity(generalSettingsIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "Không thể mở cài đặt ứng dụng.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}