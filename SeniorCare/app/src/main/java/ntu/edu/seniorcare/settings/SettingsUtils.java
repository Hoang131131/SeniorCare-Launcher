package ntu.edu.seniorcare.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsUtils {

    private static final String PREFS_NAME = "LauncherSettings";
    private static final String KEY_ICON_SIZE_PERCENTAGE = "icon_size_percentage";
    private static final String KEY_TEXT_SIZE_PERCENTAGE = "text_size_percentage";
    private static final String KEY_NUM_COLUMNS = "num_columns";
    private static final String KEY_SELECTED_APPS_JSON = "selected_apps_json";

    // Default values
    private static final int DEFAULT_ICON_SIZE_PERCENTAGE = 100; // Đặt giá trị mặc định là 150
    private static final int DEFAULT_TEXT_SIZE_PERCENTAGE = 100; // Đặt giá trị mặc định là 150
    static final int DEFAULT_NUM_COLUMNS = 3;

    static final int MAX_TEXT_4_COLUMNS = 160;
    static final int MAX_ICON_4_COLUMNS = 110;
    static final int MAX_TEXT_3_COLUMNS = 220;
    static final int MAX_ICON_3_COLUMNS = 160;

    // Increased Max values for very large icons/text
    public static final int MIN_ICON_SIZE_PERCENTAGE = 100; // Nhỏ nhất 100
    public static final int MAX_ICON_SIZE_PERCENTAGE = 240; // Lớn nhất 300
    public static final int MIN_TEXT_SIZE_PERCENTAGE = 100; // Nhỏ nhất 100
    public static final int MAX_TEXT_SIZE_PERCENTAGE = 300; // Lớn nhất 300

    public static final int MIN_NUM_COLUMNS = 2; // For clarity, though not directly used here
    public static final int MAX_NUM_COLUMNS = 4; // For clarity, though not directly used here

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Icon Size
    public static void saveIconSizePercentage(Context context, int percentage) {
        getSharedPreferences(context).edit().putInt(KEY_ICON_SIZE_PERCENTAGE, percentage).apply();
    }

    public static int getIconSizePercentage(Context context) {
        return getSharedPreferences(context).getInt(KEY_ICON_SIZE_PERCENTAGE, DEFAULT_ICON_SIZE_PERCENTAGE);
    }

    // Text Size
    public static void saveTextSizePercentage(Context context, int percentage) {
        getSharedPreferences(context).edit().putInt(KEY_TEXT_SIZE_PERCENTAGE, percentage).apply();
    }

    public static int getTextSizePercentage(Context context) {
        return getSharedPreferences(context).getInt(KEY_TEXT_SIZE_PERCENTAGE, DEFAULT_TEXT_SIZE_PERCENTAGE);
    }

    // Number of Columns
    public static void saveNumColumns(Context context, int numColumns) {
        // Ensure that saved numColumns is within valid range (2-4)
        numColumns = Math.max(MIN_NUM_COLUMNS, Math.min(MAX_NUM_COLUMNS, numColumns));
        getSharedPreferences(context).edit().putInt(KEY_NUM_COLUMNS, numColumns).apply();
    }

    public static int getNumColumns(Context context) {
        return getSharedPreferences(context).getInt(KEY_NUM_COLUMNS, DEFAULT_NUM_COLUMNS);
    }

    // Selected Apps JSON
    public static void saveSelectedAppsJson(Context context, String json) {
        getSharedPreferences(context).edit().putString(KEY_SELECTED_APPS_JSON, json).apply();
    }

    public static String getSelectedAppsJson(Context context) {
        return getSharedPreferences(context).getString(KEY_SELECTED_APPS_JSON, "[]");
    }
}