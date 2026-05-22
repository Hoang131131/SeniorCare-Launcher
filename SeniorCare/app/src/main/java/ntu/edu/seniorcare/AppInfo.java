package ntu.edu.seniorcare;

import android.graphics.drawable.Drawable;
import java.io.Serializable;

public class AppInfo implements Serializable {

    private String appName;
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

    public Drawable getAppIcon() {
        return appIcon;
    }

    public String getPackageName() {
        return packageName;
    }

    public AppInfo() {
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return packageName.equals(appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }
}
