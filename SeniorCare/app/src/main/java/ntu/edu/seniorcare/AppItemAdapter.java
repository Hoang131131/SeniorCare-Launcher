// AppItemAdapter.java
package ntu.edu.seniorcare;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppItemAdapter extends RecyclerView.Adapter<AppItemAdapter.AppItemViewHolder> {

    private List<AppItem> appItemList;
    private OnAppClickListener listener;
    private SharedPreferences sharedPreferences;
    private Context context; // Để truy cập SharedPreferences

    public interface OnAppClickListener {
        void onAppClick(AppItem appItem);
    }

    public AppItemAdapter(List<AppItem> appItemList, OnAppClickListener listener) {
        this.appItemList = appItemList;
        this.listener = listener;
        // Context sẽ được khởi tạo trong onCreateViewHolder
    }

    @NonNull
    @Override
    public AppItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext(); // Lấy context ở đây
        sharedPreferences = context.getSharedPreferences(LauncherSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_item_layout, parent, false);
        return new AppItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppItemViewHolder holder, int position) {
        AppItem appItem = appItemList.get(position);
        holder.appIcon.setImageDrawable(appItem.getIcon());
        holder.appLabel.setText(appItem.getLabel());

        // Áp dụng kích thước chữ và icon từ cài đặt
        applySettingsToViewHolder(holder);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppClick(appItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appItemList.size();
    }

    private void applySettingsToViewHolder(AppItemViewHolder holder) {
        int fontSizeSetting = sharedPreferences.getInt(LauncherSettingsActivity.KEY_FONT_SIZE, LauncherSettingsActivity.FONT_SIZE_MEDIUM);
        float textSizeSp;
        switch (fontSizeSetting) {
            case LauncherSettingsActivity.FONT_SIZE_SMALL:
                textSizeSp = 14f;
                break;
            case LauncherSettingsActivity.FONT_SIZE_LARGE:
                textSizeSp = 22f;
                break;
            case LauncherSettingsActivity.FONT_SIZE_MEDIUM:
            default:
                textSizeSp = 18f;
                break;
        }
        holder.appLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);

        int iconSizeSetting = sharedPreferences.getInt(LauncherSettingsActivity.KEY_ICON_SIZE, LauncherSettingsActivity.ICON_SIZE_MEDIUM);
        int iconPx;
        switch (iconSizeSetting) {
            case LauncherSettingsActivity.ICON_SIZE_SMALL:
                iconPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
                break;
            case LauncherSettingsActivity.ICON_SIZE_LARGE:
                iconPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, context.getResources().getDisplayMetrics());
                break;
            case LauncherSettingsActivity.ICON_SIZE_MEDIUM:
            default:
                iconPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics());
                break;
        }
        ViewGroup.LayoutParams layoutParams = holder.appIcon.getLayoutParams();
        layoutParams.width = iconPx;
        layoutParams.height = iconPx;
        holder.appIcon.setLayoutParams(layoutParams);
    }

    public static class AppItemViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appLabel;

        public AppItemViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appLabel = itemView.findViewById(R.id.app_label);
        }
    }
}