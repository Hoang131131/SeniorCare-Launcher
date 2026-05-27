package ntu.edu.seniorcare;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public interface OnClickListener {
        void onClick(int position, AppInfo appInfo);
    }

    private OnClickListener onClickListener;

    private Context context;
    private List<AppInfo> appList;
    private float iconSizeFactor = 1.0f;
    private float textSizeFactor = 1.0f;

    public AppAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
    }

    public void setAppList(List<AppInfo> appList) {
        this.appList = appList;
    }

    public void setIconSizeFactor(float iconSizeFactor) {
        this.iconSizeFactor = iconSizeFactor;
    }

    public void setTextSizeFactor(float textSizeFactor) {
        this.textSizeFactor = textSizeFactor;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public AppInfo getItem(int position) {
        if (position >= 0 && position < appList.size()) {
            return appList.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.appName.setText(app.getAppName());

        // ... (phần code xử lý icon không đổi) ...
        Drawable appIconDrawable = app.getAppIcon();
        if (appIconDrawable == null) {
            if (app.getPackageName().equals(context.getPackageName())) {
                if (app.getClassName() != null) {
                    if (app.getClassName().equals(SmsActivity.class.getName())) {
                        appIconDrawable = context.getResources().getDrawable(R.drawable.ic_message, null);
                    } else if (app.getClassName().equals(ContactsActivity.class.getName())) {
                        appIconDrawable = context.getResources().getDrawable(R.drawable.ic_contacts, null);
                    }
                }
            }
            if (appIconDrawable == null) {
                try {
                    PackageManager pm = context.getPackageManager();
                    appIconDrawable = pm.getApplicationIcon(app.getPackageName());
                } catch (PackageManager.NameNotFoundException e) {
                    appIconDrawable = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
                }
            }
            if (appIconDrawable != null) {
                app.setAppIcon(appIconDrawable);
            }
        }
        holder.appIcon.setImageDrawable(appIconDrawable);

        int defaultIconSize = (int) context.getResources().getDimension(R.dimen.app_icon_default_size);
        ViewGroup.LayoutParams layoutParams = holder.appIcon.getLayoutParams();
        layoutParams.width = (int) (defaultIconSize * iconSizeFactor);
        layoutParams.height = (int) (defaultIconSize * iconSizeFactor);
        holder.appIcon.setLayoutParams(layoutParams);

        float defaultTextSizeSp = 14f;
        holder.appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSizeSp * textSizeFactor);


        holder.itemView.setOnClickListener(v -> {
            Log.d("AppAdapter", "Item clicked at position: " + holder.getAdapterPosition());
            AppInfo clickedApp = getItem(holder.getAdapterPosition());
            if (clickedApp != null) {
                Log.d("AppAdapter", "Clicked AppInfo: " + clickedApp.getAppName() + " | Package: " + clickedApp.getPackageName() + " | Class: " + clickedApp.getClassName());

                if (onClickListener != null) {
                    onClickListener.onClick(holder.getAdapterPosition(), clickedApp);
                } else {
                    Intent launchIntent = null;

                    if (clickedApp.getPackageName().equals(context.getPackageName()) && clickedApp.getClassName() != null) {
                        Log.d("AppAdapter", "Attempting to launch internal app: " + clickedApp.getAppName());
                        launchIntent = new Intent();
                        launchIntent.setClassName(clickedApp.getPackageName(), clickedApp.getClassName());
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        launchIntent.addCategory(Intent.CATEGORY_DEFAULT); // Thêm dòng này
                    } else {
                        Log.d("AppAdapter", "Attempting to launch external app: " + clickedApp.getAppName());
                        launchIntent = context.getPackageManager().getLaunchIntentForPackage(clickedApp.getPackageName());
                        if (launchIntent == null && clickedApp.getClassName() != null) {
                            Log.d("AppAdapter", "No direct launcher intent, trying ACTION_MAIN for external app.");
                            launchIntent = new Intent(Intent.ACTION_MAIN);
                            launchIntent.setClassName(clickedApp.getPackageName(), clickedApp.getClassName());
                            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                            launchIntent.addCategory(Intent.CATEGORY_DEFAULT); // Thêm dòng này
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                    }

                    if (launchIntent != null) {
                        Log.d("AppAdapter", "Launch Intent created. Component: " + launchIntent.getComponent() + " | Flags: " + launchIntent.getFlags() + " | Categories: " + launchIntent.getCategories()); // Cập nhật log để hiển thị categories
                        // flags này chỉ cần thiết lập 1 lần, bạn đã thêm ở trên rồi, nên bỏ dòng này đi để tránh trùng lặp
                        // launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                        try {
                            v.getContext().startActivity(launchIntent);
                            Log.d("AppAdapter", "startActivity called for: " + clickedApp.getAppName());
                        } catch (Exception e) {
                            Log.e("AppAdapter", "Failed to start activity for " + clickedApp.getAppName(), e);
                            Toast.makeText(v.getContext(), "Không thể mở ứng dụng: " + clickedApp.getAppName() + ". Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.w("AppAdapter", "Launch Intent is NULL for: " + clickedApp.getAppName());
                        Toast.makeText(v.getContext(), "Không thể tìm thấy ứng dụng để mở: " + clickedApp.getAppName(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Log.w("AppAdapter", "clickedApp is NULL at position: " + holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        ImageView notificationDot;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon_image_view);
            appName = itemView.findViewById(R.id.app_name_text_view);
            notificationDot = itemView.findViewById(R.id.notification_dot);
        }
    }
}