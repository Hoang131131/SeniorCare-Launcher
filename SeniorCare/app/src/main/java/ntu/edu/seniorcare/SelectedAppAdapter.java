package ntu.edu.seniorcare;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedAppAdapter extends RecyclerView.Adapter<SelectedAppAdapter.ViewHolder> {

    private List<AppInfo> apps;
    private Context context;
    private OnAppLongClickListener longClickListener;

    public interface OnAppLongClickListener {
        void onAppLongClick(AppInfo app);
    }

    public SelectedAppAdapter(Context context, List<AppInfo> apps, OnAppLongClickListener longClickListener) {
        this.context = context;
        this.apps = apps;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.appIcon.setImageDrawable(app.getAppIcon());
        holder.appName.setText(app.getAppName());
        // Adjust text size based on settings
        float textSizeFactor = (float) SettingsUtils.getTextSizePercentage(context) / 100f;
        holder.appName.setTextSize(14 * textSizeFactor); // Base size 14sp

        // Placeholder for notification dot
        holder.notificationDot.setVisibility(View.GONE);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onAppLongClick(app);
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            // This adapter is for display in settings, clicks here won't launch app directly
            // unless we want to, but typically not needed for a settings list.
            Toast.makeText(context, "Giữ lâu để xóa " + app.getAppName(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        View notificationDot; // Assuming app_item layout has this

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon_image_view);
            appName = itemView.findViewById(R.id.app_name_text_view);
            notificationDot = itemView.findViewById(R.id.notification_dot);
        }
    }
}