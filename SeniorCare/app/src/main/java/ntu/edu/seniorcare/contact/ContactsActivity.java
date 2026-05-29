package ntu.edu.seniorcare.contact;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import ntu.edu.seniorcare.R; // Đảm bảo import R đúng nếu package khác

public class ContactsActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100; // Đổi tên và dùng chung cho tất cả quyền

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts); // Đảm bảo sử dụng layout chính của activity

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        }

        // Kiểm tra và yêu cầu tất cả các quyền cần thiết khi activity được tạo
        checkAndRequestAllPermissions();
    }

    private void checkAndRequestAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG);
        }
        // BỎ COMMENT DÒNG NÀY ĐỂ YÊU CẦU QUYỀN CALL_PHONE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }
        // Nếu tất cả quyền đã có, Fragment sẽ tự động tải dữ liệu khi chúng được hiển thị.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) { // Sử dụng request code chung
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Một số quyền cần thiết đã bị từ chối. Một số chức năng (như hiển thị cuộc gọi nhỡ hoặc gọi lại) có thể không hoạt động.", Toast.LENGTH_LONG).show();
            } else {
                // Tùy chọn: Nếu tất cả quyền được cấp, bạn có thể buộc tải lại dữ liệu trong Fragment
                // Nhưng onResume của Fragment thường đã làm điều này.
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (navController != null && navController.navigateUp()) {
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }
}