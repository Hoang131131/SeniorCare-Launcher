package ntu.edu.seniorcare;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InstalledAppAdapter extends RecyclerView.Adapter<InstalledAppAdapter.ViewHolder> {

    private List<AppInfo> installedApps;
    private List<AppInfo> selectedApps = new ArrayList<>();
    private Context context;

    public InstalledAppAdapter(Context context, List<AppInfo> installedApps) {
        this.context = context;
        this.installedApps = installedApps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_installed_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = installedApps.get(position);
        holder.appIcon.setImageDrawable(app.getAppIcon());
        holder.appName.setText(app.getAppName());
        holder.checkBox.setOnCheckedChangeListener(null); // Clear previous listener
        holder.checkBox.setChecked(selectedApps.contains(app));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedApps.add(app);
            } else {
                selectedApps.remove(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return installedApps.size();
    }

    public List<AppInfo> getSelectedApps() {
        return selectedApps;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.installed_app_icon);
            appName = itemView.findViewById(R.id.installed_app_name);
            checkBox = itemView.findViewById(R.id.installed_app_checkbox);
        }
    }
}