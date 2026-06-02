package ntu.edu.seniorcare.contact;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log; // Import Log
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

    private static final String TAG = "ContactsActivity"; // Thêm TAG
    private static final int PERMISSIONS_REQUEST_CODE = 100; // Đổi tên và dùng chung cho tất cả quyền

    private NavController navController;

    // Danh sách quyền cần thiết cho ContactsActivity và các Fragment của nó
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE
    };

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
        // Không gọi tải dữ liệu trực tiếp ở đây, mà thông qua loadDataInCurrentFragment() sau khi quyền đã sẵn sàng
        checkAndRequestAllPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra quyền mỗi khi Activity resume.
        // Nếu quyền đã có, thông báo cho Fragment tải dữ liệu.
        if (hasAllPermissions(REQUIRED_PERMISSIONS)) {
            // Kích hoạt việc tải dữ liệu trong Fragment hiện tại
            loadDataInCurrentFragment();
        } else {
            // Nếu thiếu quyền, thông báo cho Fragment hiển thị trạng thái thiếu quyền
            notifyFragmentsPermissionDenied();
        }
    }

    private boolean hasAllPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void checkAndRequestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        } else {
            // Nếu đã có tất cả quyền, tải dữ liệu ngay lập tức (khi Activity vừa tạo xong)
            loadDataInCurrentFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Tất cả quyền đã được cấp.", Toast.LENGTH_SHORT).show();
                // Quyền đã được cấp, tải dữ liệu trong Fragment
                loadDataInCurrentFragment();
            } else {
                Toast.makeText(this, "Một số quyền cần thiết đã bị từ chối. Một số chức năng có thể không hoạt động.", Toast.LENGTH_LONG).show();
                // Quyền bị từ chối, có thể thông báo cho Fragment hiển thị thông báo lỗi
                notifyFragmentsPermissionDenied();
            }
        }
    }

    // Phương thức để kích hoạt việc tải dữ liệu trong Fragment hiện tại
    public void loadDataInCurrentFragment() {
        if (navController == null) return;
        // Lấy Fragment hiện tại đang hiển thị
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)
                .getChildFragmentManager().getPrimaryNavigationFragment();

        if (currentFragment instanceof PermissionAwareFragment) {
            ((PermissionAwareFragment) currentFragment).onPermissionsGranted();
        } else {
            Log.d(TAG, "Current fragment is not PermissionAwareFragment: " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
        }
    }

    // Phương thức để thông báo cho Fragment khi quyền bị từ chối
    private void notifyFragmentsPermissionDenied() {
        if (navController == null) return;
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)
                .getChildFragmentManager().getPrimaryNavigationFragment();

        if (currentFragment instanceof PermissionAwareFragment) {
            ((PermissionAwareFragment) currentFragment).onPermissionsDenied();
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