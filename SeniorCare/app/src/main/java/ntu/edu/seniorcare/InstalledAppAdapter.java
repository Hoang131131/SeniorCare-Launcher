package ntu.edu.seniorcare;

import android.content.Context;
import android.content.pm.PackageManager;
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
import java.util.Set;

public class InstalledAppAdapter extends RecyclerView.Adapter<InstalledAppAdapter.ViewHolder> {

    private final Context context;
    private final List<AppInfo> appList;
    private final List<AppInfo> selectedApps; // List to hold currently selected apps in the dialog

    public InstalledAppAdapter(Context context, List<AppInfo> appList, Set<String> preSelectedPackageNames) {
        this.context = context;
        this.appList = appList;
        this.selectedApps = new ArrayList<>();

        // Initialize selectedApps based on preSelectedPackageNames
        for (AppInfo app : appList) {
            if (preSelectedPackageNames.contains(app.getPackageName())) {
                selectedApps.add(app);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_checkable, parent, false);
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

        // Set checkbox state based on whether the app is in selectedApps
        holder.appCheckBox.setChecked(selectedApps.contains(app));

        holder.itemView.setOnClickListener(v -> {
            holder.appCheckBox.setChecked(!holder.appCheckBox.isChecked()); // Toggle checkbox
            if (holder.appCheckBox.isChecked()) {
                if (!selectedApps.contains(app)) {
                    selectedApps.add(app);
                }
            } else {
                selectedApps.remove(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public List<AppInfo> getSelectedApps() {
        return selectedApps;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox appCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon_image_view);
            appName = itemView.findViewById(R.id.app_name_text_view);
            appCheckBox = itemView.findViewById(R.id.app_checkbox);
        }
    }
}