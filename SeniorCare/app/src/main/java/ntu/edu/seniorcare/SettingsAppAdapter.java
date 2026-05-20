// SettingsAppAdapter.java
package ntu.edu.seniorcare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Set;

public class SettingsAppAdapter extends RecyclerView.Adapter<SettingsAppAdapter.SettingsAppViewHolder> {

    private List<AppItem> appItemList;
    private Set<String> hiddenAppPackageNames;
    private OnAppCheckChangeListener listener;

    public interface OnAppCheckChangeListener {
        void onAppCheckChanged(AppItem app, boolean isChecked);
    }

    public SettingsAppAdapter(List<AppItem> appItemList, Set<String> hiddenAppPackageNames, OnAppCheckChangeListener listener) {
        this.appItemList = appItemList;
        this.hiddenAppPackageNames = hiddenAppPackageNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SettingsAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.settings_app_item, parent, false);
        return new SettingsAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsAppViewHolder holder, int position) {
        AppItem appItem = appItemList.get(position);
        holder.appIcon.setImageDrawable(appItem.getIcon());
        holder.appName.setText(appItem.getLabel());

        boolean isHidden = hiddenAppPackageNames.contains(appItem.getPackageName());
        holder.checkBoxShowApp.setChecked(!isHidden); // Checkbox là "Hiển thị", nên ngược với "Ẩn"

        holder.checkBoxShowApp.setOnCheckedChangeListener(null); // Xóa listener cũ để tránh loop
        holder.checkBoxShowApp.setChecked(!isHidden);
        holder.checkBoxShowApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onAppCheckChanged(appItem, !isChecked); // true nếu muốn ẩn (isChecked=false)
            }
        });
    }

    @Override
    public int getItemCount() {
        return appItemList.size();
    }

    public static class SettingsAppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox checkBoxShowApp;

        public SettingsAppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.settings_app_icon);
            appName = itemView.findViewById(R.id.settings_app_name);
            checkBoxShowApp = itemView.findViewById(R.id.checkBoxShowApp);
        }
    }
}