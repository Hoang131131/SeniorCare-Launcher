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
import android.widget.TextView; // Import TextView
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ntu.edu.seniorcare.R;
import ntu.edu.seniorcare.contact.PermissionAwareFragment; // Import interface

public class MissedCallsFragment extends Fragment implements PermissionAwareFragment {

    private static final String TAG = "MissedCallsFragment";

    private RecyclerView missedCallsRecyclerView;
    private TextView noMissedCallsTextView; // Thêm TextView
    private MissedCallAdapter missedCallAdapter;
    private List<MissedCallGroupItem> groupedMissedCallItems;

    private Map<String, String> contactsMap;
    private ExecutorService executorService;

    public MissedCallsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        contactsMap = new HashMap<>();
        groupedMissedCallItems = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_missed_calls, container, false);

        missedCallsRecyclerView = view.findViewById(R.id.missed_calls_recycler_view);
        noMissedCallsTextView = view.findViewById(R.id.no_missed_calls_text); // Gán TextView

        if (missedCallsRecyclerView == null) {
            Log.e(TAG, "missedCallsRecyclerView is null! Check fragment_missed_calls.xml layout file for id @+id/missed_calls_recycler_view.");
        }
        if (noMissedCallsTextView == null) {
            Log.e(TAG, "noMissedCallsTextView is null! Check fragment_missed_calls.xml layout file for id @+id/no_missed_calls_text.");
        }

        if (missedCallsRecyclerView != null) {
            missedCallsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            missedCallAdapter = new MissedCallAdapter(getContext(), groupedMissedCallItems);
            missedCallsRecyclerView.setAdapter(missedCallAdapter);
        }

        // Đặt trạng thái ban đầu: chờ hoặc chưa có quyền
        if (noMissedCallsTextView != null) {
            noMissedCallsTextView.setText("Đang chờ cấp quyền để hiển thị cuộc gọi nhỡ...");
            noMissedCallsTextView.setVisibility(View.VISIBLE);
        }
        if (missedCallsRecyclerView != null) {
            missedCallsRecyclerView.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof ntu.edu.seniorcare.contact.ContactsActivity) {
            ((ntu.edu.seniorcare.contact.ContactsActivity) getActivity()).loadDataInCurrentFragment();
        }
    }

    // Triển khai interface PermissionAwareFragment
    @Override
    public void onPermissionsGranted() {
        Log.d(TAG, "onPermissionsGranted called. Loading missed calls...");
        loadContactsAndMissedCalls();
    }

    @Override
    public void onPermissionsDenied() {
        Log.w(TAG, "onPermissionsDenied called. Displaying permission error.");
        if (noMissedCallsTextView != null) {
            noMissedCallsTextView.setText("Ứng dụng cần quyền Truy cập Danh bạ, Nhật ký Cuộc gọi và Gọi điện để hiển thị cuộc gọi nhỡ. Vui lòng cấp quyền trong cài đặt ứng dụng.");
            noMissedCallsTextView.setVisibility(View.VISIBLE);
        }
        if (missedCallsRecyclerView != null) {
            missedCallsRecyclerView.setVisibility(View.GONE);
        }
        groupedMissedCallItems.clear(); // Xóa dữ liệu cũ nếu không có quyền
        if (missedCallAdapter != null) {
            missedCallAdapter.notifyDataSetChanged();
        }
    }

    private void loadContactsAndMissedCalls() {
        // Kiểm tra quyền một lần nữa trước khi thực hiện, để đảm bảo
        boolean hasReadContactsPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadCallLogPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean hasCallPhonePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;

        if (!(hasReadContactsPermission && hasReadCallLogPermission && hasCallPhonePermission)) {
            Log.w(TAG, "Permissions not granted when loadContactsAndMissedCalls() was called. Skipping.");
            onPermissionsDenied();
            return;
        }

        executorService.execute(() -> {
            contactsMap.clear();
            ContentResolver contentResolver = requireContext().getContentResolver();
            String[] contactProjection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            };
            Cursor contactCursor = null;
            try {
                contactCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        contactProjection,
                        null, null, null);

                if (contactCursor != null) {
                    int numberColumnIndex = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int nameColumnIndex = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    if (numberColumnIndex != -1 && nameColumnIndex != -1) {
                        while (contactCursor.moveToNext()) {
                            String number = contactCursor.getString(numberColumnIndex);
                            String name = contactCursor.getString(nameColumnIndex);
                            if (number != null && name != null) {
                                contactsMap.put(normalizePhoneNumber(number), name);
                            }
                        }
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when reading contacts in MissedCallsFragment: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Lỗi quyền khi đọc danh bạ trong cuộc gọi nhỡ.", Toast.LENGTH_LONG).show());
            } finally {
                if (contactCursor != null) {
                    contactCursor.close();
                }
            }


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
                    int cachedNameColumn = callLogCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);

                    if (numberColumn == -1 || dateColumn == -1 || cachedNameColumn == -1) {
                        Log.e(TAG, "One or more CallLog columns not found. Check CallLog.Calls constants.");
                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi: Không tìm thấy cột dữ liệu nhật ký cuộc gọi.", Toast.LENGTH_LONG).show());
                        return;
                    }

                    do {
                        String number = callLogCursor.getString(numberColumn);
                        long callDate = callLogCursor.getLong(dateColumn);
                        String cachedName = callLogCursor.getString(cachedNameColumn);

                        String normalizedNumber = normalizePhoneNumber(number);

                        if (contactsMap.containsKey(normalizedNumber) || (cachedName != null && !cachedName.isEmpty())) {
                            String nameToDisplay = contactsMap.getOrDefault(normalizedNumber, cachedName);
                            if (nameToDisplay == null || nameToDisplay.isEmpty()) {
                                nameToDisplay = number;
                            }
                            rawMissedCalls.add(new MissedCallInfo(nameToDisplay, number, callDate));
                        }
                    } while (callLogCursor.moveToNext());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Lỗi quyền khi đọc nhật ký cuộc gọi: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Quyền đọc nhật ký cuộc gọi bị từ chối.", Toast.LENGTH_LONG).show());
                requireActivity().runOnUiThread(this::onPermissionsDenied);
            } finally {
                if (callLogCursor != null) {
                    callLogCursor.close();
                }
            }

            requireActivity().runOnUiThread(() -> {
                groupedMissedCallItems.clear();
                if (!rawMissedCalls.isEmpty()) {
                    noMissedCallsTextView.setVisibility(View.GONE);
                    missedCallsRecyclerView.setVisibility(View.VISIBLE);
                    groupMissedCallsByDate(rawMissedCalls);
                } else {
                    noMissedCallsTextView.setText("Không có cuộc gọi nhỡ nào để hiển thị hoặc quyền truy cập bị từ chối.");
                    noMissedCallsTextView.setVisibility(View.VISIBLE);
                    missedCallsRecyclerView.setVisibility(View.GONE);
                }
                if (missedCallAdapter != null) {
                    missedCallAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void groupMissedCallsByDate(List<MissedCallInfo> rawMissedCalls) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentHeaderDate = "";

        Collections.sort(rawMissedCalls, (o1, o2) -> Long.compare(o2.getCallDate(), o1.getCallDate()));

        for (MissedCallInfo call : rawMissedCalls) {
            String callDate = sdf.format(new Date(call.getCallDate()));
            if (!callDate.equals(currentHeaderDate)) {
                groupedMissedCallItems.add(new MissedCallHeaderItem(callDate));
                currentHeaderDate = callDate;
            }
            groupedMissedCallItems.add(new MissedCallDetailItem(call));
        }
    }


    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        String cleanedNumber = phoneNumber.replaceAll("[^\\d+]", "");
        if (cleanedNumber.startsWith("+") && cleanedNumber.length() > 1) {
            cleanedNumber = "+" + cleanedNumber.substring(1).replaceAll("[^\\d]", "");
        } else {
            cleanedNumber = cleanedNumber.replaceAll("[^\\d]", "");
        }

        if (cleanedNumber.startsWith("+") && cleanedNumber.length() >= 10) {
            return cleanedNumber;
        }

        if (cleanedNumber.startsWith("0") && cleanedNumber.length() >= 9) {
            return "+84" + cleanedNumber.substring(1);
        } else if (cleanedNumber.startsWith("84") && cleanedNumber.length() >= 9) {
            return "+" + cleanedNumber;
        }

        if (cleanedNumber.length() >= 9 && cleanedNumber.length() <= 11) {
            return "+84" + cleanedNumber;
        }

        return cleanedNumber;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}