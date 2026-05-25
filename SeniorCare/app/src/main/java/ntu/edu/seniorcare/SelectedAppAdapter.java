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
    private final OnAppRemovedListener listener; // Giữ lại listener như bạn đã khai báo

    public interface OnAppRemovedListener {
        void onAppRemoved(AppInfo app);
    }

    public SelectedAppAdapter(Context context, List<AppInfo> appList, OnAppRemovedListener listener) {
        this.context = context;
        this.appList = appList;
        this.listener = listener; // Khởi tạo listener
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

        Drawable appIconDrawable = app.getAppIcon(); // Lấy icon từ AppInfo đã được tải (có thể là null)
        if (appIconDrawable == null) { // Nếu icon là null (chưa được tải hoặc transient)
            // Kiểm tra xem đây có phải là ứng dụng nội bộ của chúng ta không
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
            // Nếu vẫn chưa có icon (hoặc là ứng dụng bên ngoài), thử tải từ PackageManager
            if (appIconDrawable == null) {
                try {
                    PackageManager pm = context.getPackageManager();
                    appIconDrawable = pm.getApplicationIcon(app.getPackageName());
                } catch (PackageManager.NameNotFoundException e) {
                    // Fallback nếu không tìm thấy icon hoặc ứng dụng
                    appIconDrawable = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
                }
            }
            // Cập nhật lại icon vào đối tượng AppInfo để không cần tải lại nữa trong các lần onBindViewHolder tiếp theo
            if (appIconDrawable != null) {
                app.setAppIcon(appIconDrawable);
            }
        }
        // Gán icon đã có hoặc đã tải vào ImageView
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

        // Giữ nguyên onClickListener như phiên bản của bạn, nhưng cần lưu ý:
        // Intent này sử dụng getLaunchIntentForPackage(), cái này có thể KHÔNG hoạt động
        // cho các Activity nội bộ như SmsActivity và ContactsActivity (vì chúng không
        // có LAUNCHER category). Nếu bạn muốn mở từ đây, bạn cần sửa Intent này.
        // Tuy nhiên, ưu tiên hiện tại là fix lỗi icon.
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