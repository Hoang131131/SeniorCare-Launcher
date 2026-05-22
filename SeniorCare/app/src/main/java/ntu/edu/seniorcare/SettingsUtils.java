package ntu.edu.seniorcare;

import android.content.Context;
import android.content.SharedPreferences;

// Utility class for managing settings
public class SettingsUtils {

    private static final String PREFS_NAME = "launcher_settings";
    private static final String KEY_ICON_SIZE_PERCENTAGE = "icon_size_percentage";
    private static final String KEY_TEXT_SIZE_PERCENTAGE = "text_size_percentage";
    private static final String KEY_NUM_COLUMNS = "num_columns";
    private static final String KEY_SELECTED_APPS = "selected_apps"; // Key for selected apps JSON

    public static void saveIconSizePercentage(Context context, int percentage) {
        getPrefs(context).edit().putInt(KEY_ICON_SIZE_PERCENTAGE, percentage).apply();
    }

    public static int getIconSizePercentage(Context context) {
        return getPrefs(context).getInt(KEY_ICON_SIZE_PERCENTAGE, 50); // Default 50%
    }

    public static void saveTextSizePercentage(Context context, int percentage) {
        getPrefs(context).edit().putInt(KEY_TEXT_SIZE_PERCENTAGE, percentage).apply();
    }

    public static int getTextSizePercentage(Context context) {
        return getPrefs(context).getInt(KEY_TEXT_SIZE_PERCENTAGE, 50); // Default 50%
    }

    public static void saveNumColumns(Context context, int columns) {
        getPrefs(context).edit().putInt(KEY_NUM_COLUMNS, columns).apply();
    }

    public static int getNumColumns(Context context) {
        return getPrefs(context).getInt(KEY_NUM_COLUMNS, 4); // Default 4 columns
    }

    // For saving/loading the JSON string of selected apps
    public static void saveSelectedAppsJson(Context context, String json) {
        getPrefs(context).edit().putString(KEY_SELECTED_APPS, json).apply();
    }

    public static String getSelectedAppsJson(Context context) {
        return getPrefs(context).getString(KEY_SELECTED_APPS, null);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}