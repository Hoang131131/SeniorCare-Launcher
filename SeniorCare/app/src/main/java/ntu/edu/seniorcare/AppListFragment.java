package ntu.edu.seniorcare;

import android.content.Context;
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
    private List<AppItem> appList = new ArrayList<>(); // Khởi tạo ngay tại đây để tránh NPE
    private AppItemAdapter.OnAppClickListener appClickListener;

    public AppListFragment() {
        // Required empty public constructor
    }

    public static AppListFragment newInstance(AppItemAdapter.OnAppClickListener listener) {
        AppListFragment fragment = new AppListFragment();
        fragment.appClickListener = listener;
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (appClickListener == null && context instanceof AppItemAdapter.OnAppClickListener) {
            appClickListener = (AppItemAdapter.OnAppClickListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // appList đã được khởi tạo ở trên
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);

        recyclerView = view.findViewById(R.id.app_list_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new AppItemAdapter(appList, appClickListener);
        recyclerView.setAdapter(adapter);

        return view;
    }

    public void setAppsForPage(List<AppItem> apps) {
        if (this.appList == null) {
            this.appList = new ArrayList<>();
        }
        this.appList.clear();
        this.appList.addAll(apps);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void updateLayout() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}