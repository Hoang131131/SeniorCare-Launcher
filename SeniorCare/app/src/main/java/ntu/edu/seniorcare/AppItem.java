package ntu.edu.seniorcare;


import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class AppItem implements Parcelable {
    private String label;
    private Drawable icon; // Lưu ý: Drawable không thể Parcelable trực tiếp
    private String packageName;
    private boolean isSystemApp;

    // Constructor mới cho Parcelable
    protected AppItem(Parcel in) {
        label = in.readString();
        // icon không thể đọc trực tiếp từ Parcel (xem ghi chú bên dưới)
        packageName = in.readString();
        isSystemApp = in.readByte() != 0;
    }

    public AppItem(String label, Drawable icon, String packageName, boolean isSystemApp) {
        this.label = label;
        this.icon = icon;
        this.packageName = packageName;
        this.isSystemApp = isSystemApp;
    }

    public String getLabel() { return label; }
    public Drawable getIcon() { return icon; }
    public String getPackageName() { return packageName; }
    public boolean isSystemApp() { return isSystemApp; }

    public void setIcon(Drawable icon) { this.icon = icon; } // Thêm setter để có thể gán lại icon sau

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        // Bạn KHÔNG THỂ ghi Drawable trực tiếp vào Parcel.
        // Nếu muốn lưu icon, bạn cần chuyển nó thành byte array (ví dụ Bitmap)
        // hoặc chỉ lưu tên resource ID nếu là icon tĩnh.
        // Với icon ứng dụng, thường chúng ta sẽ load lại bằng packageName và PackageManager.
        dest.writeString(packageName);
        dest.writeByte((byte) (isSystemApp ? 1 : 0));
    }

    public static final Creator<AppItem> CREATOR = new Creator<AppItem>() {
        @Override
        public AppItem createFromParcel(Parcel in) {
            return new AppItem(in);
        }

        @Override
        public AppItem[] newArray(int size) {
            return new AppItem[size];
        }
    };
}