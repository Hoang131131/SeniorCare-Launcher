// YourAppPagerAdapter.java
package ntu.edu.seniorcare; // Đảm bảo đúng package của bạn

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class YourAppPagerAdapter extends FragmentStateAdapter {

    private List<List<AppItem>> pagedApps; // Danh sách các trang ứng dụng
    private AppItemAdapter.OnAppClickListener appClickListener; // Listener để truyền xuống Fragment

    public YourAppPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                               List<List<AppItem>> pagedApps,
                               AppItemAdapter.OnAppClickListener appClickListener) {
        super(fragmentActivity);
        this.pagedApps = pagedApps;
        this.appClickListener = appClickListener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Tạo một AppListFragment mới và truyền danh sách ứng dụng cho trang đó SAU KHI TẠO
        AppListFragment fragment = AppListFragment.newInstance(appClickListener); // Chỉ truyền listener
        // Sau đó, set danh sách ứng dụng cho fragment
        fragment.setAppsForPage(pagedApps.get(position));
        return fragment;
    }

    @Override
    public int getItemCount() {
        return pagedApps.size();
    }
}