package ntu.edu.seniorcare;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.Objects;

public class AppInfo implements Serializable {
    private String appName;
    // transient modifier tells Gson to ignore this field during serialization/deserialization
    private transient Drawable appIcon;
    private String packageName;

    public AppInfo(String appName, Drawable appIcon, String packageName) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    // Override equals and hashCode based on package name for proper list comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return packageName.equals(appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName);
    }
}