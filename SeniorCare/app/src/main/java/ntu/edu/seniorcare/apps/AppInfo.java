package ntu.edu.seniorcare.apps;

import android.graphics.drawable.Drawable;
import java.io.Serializable;
import java.util.Objects;

public class AppInfo implements Serializable {
    private String appName;
    // transient modifier tells Gson to ignore this field during serialization/deserialization
    private transient Drawable appIcon;
    private String packageName;
    private String className;

    // Chỉ giữ một constructor duy nhất
    public AppInfo(String appName, Drawable appIcon, String packageName, String className) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.packageName = packageName;
        this.className = className;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return packageName.equals(appInfo.packageName) && className.equals(appInfo.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, className);
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "appName='" + appName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}