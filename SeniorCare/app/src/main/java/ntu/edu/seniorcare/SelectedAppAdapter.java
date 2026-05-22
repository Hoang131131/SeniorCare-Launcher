package ntu.edu.seniorcare;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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
        // Corrected to use item_selected_app.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.appName.setText(app.getAppName());

        try {
            PackageManager pm = context.getPackageManager();
            Drawable appIcon = pm.getApplicationIcon(app.getPackageName());
            holder.appIcon.setImageDrawable(appIcon);
        } catch (PackageManager.NameNotFoundException e) {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon); // Fallback icon
            e.printStackTrace();
        }

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
            // Optional: Launch app on short click, or do nothing as per requirement
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Không thể mở ứng dụng: " + app.getAppName(), Toast.LENGTH_SHORT).show();
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
            // Corrected to use IDs from item_selected_app.xml
            appIcon = itemView.findViewById(R.id.selected_app_icon);
            appName = itemView.findViewById(R.id.selected_app_name);
        }
    }
}