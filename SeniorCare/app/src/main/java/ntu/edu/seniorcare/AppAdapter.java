package ntu.edu.seniorcare;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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

    private Context context;
    private List<AppInfo> appList;
    private float iconSizeFactor = 1.0f; // Default 100%
    private float textSizeFactor = 1.0f; // Default 100%

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

        try {
            PackageManager pm = context.getPackageManager();
            Drawable appIcon = pm.getApplicationIcon(app.getPackageName());
            holder.appIcon.setImageDrawable(appIcon);
        } catch (PackageManager.NameNotFoundException e) {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon); // Fallback icon
            e.printStackTrace();
        }

        // Apply dynamic icon size
        // Use an existing dimension for the default size to scale from
        int defaultIconSize = (int) context.getResources().getDimension(R.dimen.app_icon_default_size);

        ViewGroup.LayoutParams layoutParams = holder.appIcon.getLayoutParams();
        layoutParams.width = (int) (defaultIconSize * iconSizeFactor);
        layoutParams.height = (int) (defaultIconSize * iconSizeFactor);
        holder.appIcon.setLayoutParams(layoutParams);

        // Apply dynamic text size
        // Assuming a default text size (e.g., 14sp) to scale from
        float defaultTextSizeSp = 14f;
        holder.appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSizeSp * textSizeFactor);


        holder.itemView.setOnClickListener(v -> {
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

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        ImageView notificationDot; // Still present for future notification logic

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon_image_view);
            appName = itemView.findViewById(R.id.app_name_text_view);
            notificationDot = itemView.findViewById(R.id.notification_dot);
        }
    }
}