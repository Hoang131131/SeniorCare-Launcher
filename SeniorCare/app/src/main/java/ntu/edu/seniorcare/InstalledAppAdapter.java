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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InstalledAppAdapter extends RecyclerView.Adapter<InstalledAppAdapter.ViewHolder> {

    private Context context;
    private List<AppInfo> appList;
    private Set<String> selectedAppIdentifiers;

    public InstalledAppAdapter(Context context, List<AppInfo> appList, Set<String> currentlySelectedAppIdentifiers) {
        this.context = context;
        this.appList = appList;
        this.selectedAppIdentifiers = new HashSet<>(currentlySelectedAppIdentifiers);
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

        String appIdentifier = app.getPackageName() + "/" + app.getClassName();
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedAppIdentifiers.contains(appIdentifier));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedAppIdentifiers.add(appIdentifier);
            } else {
                selectedAppIdentifiers.remove(appIdentifier);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public List<AppInfo> getSelectedApps() {
        List<AppInfo> selected = new ArrayList<>();
        for (AppInfo app : appList) {
            String appIdentifier = app.getPackageName() + "/" + app.getClassName();
            if (selectedAppIdentifiers.contains(appIdentifier)) {
                // Tạo một AppInfo MỚI, truyền null cho icon. Gson sẽ bỏ qua nó vì transient.
                selected.add(new AppInfo(app.getAppName(), null, app.getPackageName(), app.getClassName()));
            }
        }
        return selected;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon_image_view);
            appName = itemView.findViewById(R.id.app_name_text_view);
            checkBox = itemView.findViewById(R.id.app_checkbox);
        }
    }
}