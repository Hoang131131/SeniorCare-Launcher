package ntu.edu.seniorcare.contact.missedcalls;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ntu.edu.seniorcare.R;

public class MissedCallsFragment extends Fragment {

    private static final String TAG = "MissedCallsFragment";
    private static final int PERMISSIONS_REQUEST_FOR_MISSED_CALLS = 300; // Request code riêng cho Fragment này, nếu cần

    private RecyclerView missedCallsRecyclerView;
    private TextView noMissedCallsTextView;
    private MissedCallAdapter missedCallAdapter;
    private List<MissedCallGroupItem> groupedMissedCallItems; // Danh sách chứa cả header và item cuộc gọi

    private Map<String, String> contactsMap; // key: normalized number, value: contact name
    private ExecutorService executorService; // Để thực hiện các tác vụ đọc dữ liệu trên background thread

    public MissedCallsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor(); // Khởi tạo ExecutorService
        contactsMap = new HashMap<>();
        groupedMissedCallItems = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_missed_calls, container, false);

        missedCallsRecyclerView = view.findViewById(R.id.missed_calls_recycler_view);
        noMissedCallsTextView = view.findViewById(R.id.no_missed_calls_text);
        missedCallsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        missedCallAdapter = new MissedCallAdapter(getContext(), groupedMissedCallItems);
        missedCallsRecyclerView.setAdapter(missedCallAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra quyền và tải dữ liệu mỗi khi Fragment resume
        checkPermissionsAndLoadAllData();
    }

    private void checkPermissionsAndLoadAllData() {
        boolean hasReadContactsPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadCallLogPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean hasCallPhonePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;

        // Nếu tất cả quyền cần thiết đã được cấp, thì tải dữ liệu
        if (hasReadContactsPermission && hasReadCallLogPermission && hasCallPhonePermission) {
            loadContactsAndMissedCalls();
        } else {
            // Nếu không, chỉ hiển thị thông báo "Không có cuộc gọi nhỡ" hoặc một thông báo khác
            noMissedCallsTextView.setText("Ứng dụng cần quyền Truy cập Danh bạ và Nhật ký Cuộc gọi để hiển thị cuộc gọi nhỡ.");
            noMissedCallsTextView.setVisibility(View.VISIBLE);
            missedCallsRecyclerView.setVisibility(View.GONE);
            groupedMissedCallItems.clear(); // Xóa dữ liệu cũ nếu không có quyền
            missedCallAdapter.notifyDataSetChanged();
            // Yêu cầu quyền nếu chưa được cấp (Chỉ yêu cầu nếu người dùng chưa từ chối vĩnh viễn)
            // Tuy nhiên, việc yêu cầu quyền chính đã được thực hiện ở Activity, Fragment chỉ cần phản ứng.
            // Có thể hiển thị một dialog nhỏ hướng dẫn người dùng vào cài đặt để cấp quyền.
        }
    }

    private void loadContactsAndMissedCalls() {
        // Thực hiện việc tải dữ liệu trên một background thread
        executorService.execute(() -> {
            // Tải danh bạ
            contactsMap.clear();
            ContentResolver contentResolver = requireContext().getContentResolver();
            String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            };
            Cursor cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null, null, null);

            if (cursor != null) {
                int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                if (numberColumnIndex != -1 && nameColumnIndex != -1) {
                    while (cursor.moveToNext()) {
                        String number = cursor.getString(numberColumnIndex);
                        String name = cursor.getString(nameColumnIndex);
                        if (number != null && name != null) {
                            contactsMap.put(normalizePhoneNumber(number), name);
                        }
                    }
                }
                cursor.close();
            }

            // Tải cuộc gọi nhỡ và nhóm theo ngày
            List<MissedCallInfo> rawMissedCalls = new ArrayList<>();
            ContentResolver cr = requireContext().getContentResolver();
            String selection = CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE;
            Cursor callLogCursor = null;
            try {
                callLogCursor = cr.query(
                        CallLog.Calls.CONTENT_URI,
                        null, selection, null, CallLog.Calls.DATE + " DESC");

                if (callLogCursor != null && callLogCursor.moveToFirst()) {
                    int numberColumn = callLogCursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int dateColumn = callLogCursor.getColumnIndex(CallLog.Calls.DATE);
                    int cachedNameColumn = callLogCursor.getColumnIndex(CallLog.Calls.CACHED_NAME); // Lấy tên đã cache

                    do {
                        String number = callLogCursor.getString(numberColumn);
                        long callDate = callLogCursor.getLong(dateColumn);
                        String cachedName = callLogCursor.getString(cachedNameColumn);

                        String normalizedNumber = normalizePhoneNumber(number);

                        // Chỉ thêm vào danh sách nếu số này có trong danh bạ đã tải hoặc có tên cached
                        if (contactsMap.containsKey(normalizedNumber) || (cachedName != null && !cachedName.isEmpty())) {
                            String nameToDisplay = contactsMap.getOrDefault(normalizedNumber, cachedName);
                            if (nameToDisplay == null || nameToDisplay.isEmpty()) {
                                nameToDisplay = number; // Fallback nếu không có tên
                            }
                            rawMissedCalls.add(new MissedCallInfo(nameToDisplay, number, callDate));
                        }
                    } while (callLogCursor.moveToNext());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Lỗi quyền khi đọc nhật ký cuộc gọi: " + e.getMessage());
                // Xử lý lỗi trên UI thread
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Quyền đọc nhật ký cuộc gọi bị từ chối.", Toast.LENGTH_LONG).show());
            } finally {
                if (callLogCursor != null) {
                    callLogCursor.close();
                }
            }

            // Nhóm và cập nhật UI trên Main Thread
            requireActivity().runOnUiThread(() -> {
                groupedMissedCallItems.clear();
                if (rawMissedCalls.isEmpty()) {
                    noMissedCallsTextView.setText("Không có cuộc gọi nhỡ nào từ danh bạ.");
                    noMissedCallsTextView.setVisibility(View.VISIBLE);
                    missedCallsRecyclerView.setVisibility(View.GONE);
                } else {
                    noMissedCallsTextView.setVisibility(View.GONE);
                    missedCallsRecyclerView.setVisibility(View.VISIBLE);
                    groupMissedCallsByDate(rawMissedCalls);
                }
                missedCallAdapter.notifyDataSetChanged();
            });
        });
    }

    private void groupMissedCallsByDate(List<MissedCallInfo> rawMissedCalls) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentHeaderDate = "";

        // Sắp xếp theo ngày giảm dần, sau đó theo thời gian giảm dần
        Collections.sort(rawMissedCalls, (o1, o2) -> {
            int dateCompare = Long.compare(o2.getCallDate(), o1.getCallDate()); // Mới nhất trước
            return dateCompare;
        });


        for (MissedCallInfo call : rawMissedCalls) {
            String callDate = sdf.format(new Date(call.getCallDate()));
            if (!callDate.equals(currentHeaderDate)) {
                groupedMissedCallItems.add(new MissedCallHeaderItem(callDate));
                currentHeaderDate = callDate;
            }
            groupedMissedCallItems.add(new MissedCallDetailItem(call));
        }
    }


    /**
     * Chuẩn hóa số điện thoại. Loại bỏ các ký tự không phải số và cố gắng thống nhất định dạng.
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String normalized = phoneNumber.replaceAll("[^\\d+]", ""); // Giữ dấu '+'

        // Một số logic chuẩn hóa ví dụ:
        if (normalized.startsWith("0") && normalized.length() > 9) { // Ví dụ: 09xx -> +849xx
            normalized = "+84" + normalized.substring(1);
        } else if (normalized.startsWith("84") && normalized.length() > 9) { // Ví dụ: 849xx -> +849xx
            normalized = "+" + normalized;
        }
        // Có thể thêm các quy tắc khác tùy theo yêu cầu cụ thể của bạn

        return normalized;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow(); // Đóng ExecutorService khi Fragment bị hủy
    }
}