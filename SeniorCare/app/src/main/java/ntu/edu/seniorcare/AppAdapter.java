package ntu.edu.seniorcare;

import android.content.Context;
import android.content.Intent;
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

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.app_item, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.appNameTextView.setText(app.getAppName());
        holder.appIconImageView.setImageDrawable(app.getAppIcon());

        int baseIconSize = 64;
        int newIconSize = (int) (baseIconSize * iconSizeFactor);
        holder.appIconImageView.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newIconSize, context.getResources().getDisplayMetrics());
        holder.appIconImageView.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newIconSize, context.getResources().getDisplayMetrics());
        holder.appIconImageView.requestLayout();

        float baseTextSizeSp = 14f;
        holder.appNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseTextSizeSp * textSizeFactor);

        holder.notificationDot.setVisibility(View.GONE);

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
        ImageView appIconImageView;
        TextView appNameTextView;
        View notificationDot;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIconImageView = itemView.findViewById(R.id.app_icon_image_view);
            appNameTextView = itemView.findViewById(R.id.app_name_text_view);
            notificationDot = itemView.findViewById(R.id.notification_dot);
        }
    }
}