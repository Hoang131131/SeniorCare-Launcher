package ntu.edu.seniorcare;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log; // Thêm import này
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedAppAdapter extends RecyclerView.Adapter<SelectedAppAdapter.ViewHolder> {

    private final Context context;
    private final List<AppInfo> appList;
    private final OnAppRemovedListener listener;

    public interface OnAppRemovedListener {
        void onAppRemoved(AppInfo app);
    }

    public SelectedAppAdapter(Context context, List<AppInfo> appList, OnAppRemovedListener listener) {
        this.context = context;
        this.appList = appList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.appName.setText(app.getAppName());

        Drawable appIconDrawable = app.getAppIcon();
        if (appIconDrawable == null) {
            if (app.getPackageName().equals(context.getPackageName())) {
                if (app.getClassName() != null) {
                    if (app.getClassName().equals(SmsActivity.class.getName())) {
                        try {
                            appIconDrawable = context.getResources().getDrawable(R.drawable.ic_message, null);
                        } catch (android.content.res.Resources.NotFoundException e) {
                            appIconDrawable = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
                            e.printStackTrace();
                        }
                    } else if (app.getClassName().equals(ContactsActivity.class.getName())) {
                        try {
                            appIconDrawable = context.getResources().getDrawable(R.drawable.ic_contacts, null);
                        } catch (android.content.res.Resources.NotFoundException e) {
                            appIconDrawable = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
                            e.printStackTrace();
                        }
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

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xóa ứng dụng")
                    .setMessage("Bạn có muốn xóa " + app.getAppName() + " khỏi Launcher không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (listener != null) {
                            listener.onAppRemoved(app);
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            Log.d("SelectedAppAdapter", "Item clicked: " + app.getAppName()); // Thêm log
            Intent launchIntent = null;

            if (app.getPackageName().equals(context.getPackageName()) && app.getClassName() != null) {
                launchIntent = new Intent();
                launchIntent.setClassName(app.getPackageName(), app.getClassName());
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launchIntent.addCategory(Intent.CATEGORY_DEFAULT); // Thêm dòng này
            } else {
                launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                if (launchIntent == null && app.getClassName() != null) {
                    launchIntent = new Intent(Intent.ACTION_MAIN);
                    launchIntent.setClassName(app.getPackageName(), app.getClassName());
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    launchIntent.addCategory(Intent.CATEGORY_DEFAULT); // Thêm dòng này
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
            }

            if (launchIntent != null) {
                Log.d("SelectedAppAdapter", "Launch Intent created. Component: " + launchIntent.getComponent() + " | Flags: " + launchIntent.getFlags() + " | Categories: " + launchIntent.getCategories()); // Cập nhật log
                // launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); // Bỏ dòng này

                try {
                    context.startActivity(launchIntent);
                    Log.d("SelectedAppAdapter", "startActivity called for: " + app.getAppName()); // Thêm log
                } catch (Exception e) {
                    Log.e("SelectedAppAdapter", "Failed to start activity for " + app.getAppName(), e); // Thêm log
                    Toast.makeText(context, "Không thể mở ứng dụng: " + app.getAppName() + ". Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w("SelectedAppAdapter", "Launch Intent is NULL for: " + app.getAppName()); // Thêm log
                Toast.makeText(context, "Không thể tìm thấy ứng dụng để mở: " + app.getAppName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.selected_app_icon);
            appName = itemView.findViewById(R.id.selected_app_name);
        }
    }
}