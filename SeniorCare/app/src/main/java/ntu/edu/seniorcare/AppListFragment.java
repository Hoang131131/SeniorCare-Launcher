package ntu.edu.seniorcare;

import android.content.Context; // Thêm import này
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppItemAdapter adapter;
    private List<AppItem> appList;
    private AppItemAdapter.OnAppClickListener appClickListener;

    public AppListFragment() {
        // Required empty public constructor
    }

    // Phương thức static để tạo instance mới.
    // Danh sách ứng dụng sẽ được set sau bởi PagerAdapter.
    public static AppListFragment newInstance(AppItemAdapter.OnAppClickListener listener) {
        AppListFragment fragment = new AppListFragment();
        fragment.appClickListener = listener; // Lưu listener
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Đảm bảo Activity host có implements AppItemAdapter.OnAppClickListener
        if (appClickListener == null && context instanceof AppItemAdapter.OnAppClickListener) {
            appClickListener = (AppItemAdapter.OnAppClickListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (appList == null) {
            appList = new ArrayList<>(); // Khởi tạo nếu chưa có
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);

        recyclerView = view.findViewById(R.id.app_list_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new AppItemAdapter(appList, appClickListener); // Adapter đã được cập nhật để đọc cài đặt
        recyclerView.setAdapter(adapter);

        return view;
    }

    // Phương thức này sẽ được gọi từ PagerAdapter để truyền danh sách ứng dụng cho trang hiện tại
    public void setAppsForPage(List<AppItem> apps) {
        this.appList.clear();
        this.appList.addAll(apps);
        if (adapter != null) {
            adapter.notifyDataSetChanged(); // Yêu cầu adapter vẽ lại
        }
    }

    // Phương thức để yêu cầu adapter cập nhật lại các item khi cài đặt thay đổi
    public void updateLayout() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}