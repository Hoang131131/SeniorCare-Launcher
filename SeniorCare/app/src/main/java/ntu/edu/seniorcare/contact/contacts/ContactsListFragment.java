package ntu.edu.seniorcare.contact.contacts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ntu.edu.seniorcare.R;
import ntu.edu.seniorcare.contact.PermissionAwareFragment; // Import interface

public class ContactsListFragment extends Fragment implements PermissionAwareFragment {

    private static final String TAG = "ContactsListFragment";
    // Không cần PERMISSIONS_REQUEST_READ_CONTACTS_FRAGMENT ở đây nữa

    private RecyclerView contactsRecyclerView;
    private ContactsAdapter contactsAdapter;
    private List<ContactInfo> contactList;
    private TextView noContactsTextView; // Thêm TextView

    private ExecutorService executorService;

    public ContactsListFragment() {
        // Required empty public constr uctor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        contactList = new ArrayList<>();
        // Adapter sẽ được khởi tạo trong onCreateView
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts_list, container, false);

        contactsRecyclerView = view.findViewById(R.id.contacts_recycler_view);
        noContactsTextView = view.findViewById(R.id.no_contacts_text); // Gán TextView

        if (contactsRecyclerView == null) {
            Log.e(TAG, "contactsRecyclerView is null! Check fragment_contacts_list.xml layout file for id @+id/contacts_recycler_view.");
        }
        if (noContactsTextView == null) {
            Log.e(TAG, "noContactsTextView is null! Check fragment_contacts_list.xml layout file for id @+id/no_contacts_text.");
        }

        if (contactsRecyclerView != null) {
            contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            contactsAdapter = new ContactsAdapter(getContext(), contactList);
            contactsRecyclerView.setAdapter(contactsAdapter);
        }

        // Đặt trạng thái ban đầu: chờ hoặc chưa có quyền
        if (noContactsTextView != null) {
            noContactsTextView.setText("Đang chờ cấp quyền để hiển thị danh bạ...");
            noContactsTextView.setVisibility(View.VISIBLE);
        }
        if (contactsRecyclerView != null) {
            contactsRecyclerView.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Không tự động tải ở đây, Activity sẽ gọi onPermissionsGranted/Denied
        // Nếu Activity đã có quyền, nó sẽ gọi loadDataInCurrentFragment() trong onResume của nó.
        // Tuy nhiên, nếu Fragment này đang được hiển thị khi Activity resume, chúng ta cần kích hoạt lại nó.
        // Có thể gọi lại phương thức của Activity để đảm bảo Fragment được cập nhật trạng thái quyền.
        if (getActivity() instanceof ntu.edu.seniorcare.contact.ContactsActivity) {
            ((ntu.edu.seniorcare.contact.ContactsActivity) getActivity()).loadDataInCurrentFragment();
        }
    }

    // Triển khai interface PermissionAwareFragment
    @Override
    public void onPermissionsGranted() {
        Log.d(TAG, "onPermissionsGranted called. Loading contacts...");
        loadContacts();
    }

    @Override
    public void onPermissionsDenied() {
        Log.w(TAG, "onPermissionsDenied called. Displaying permission error.");
        if (noContactsTextView != null) {
            noContactsTextView.setText("Ứng dụng cần quyền truy cập danh bạ để hiển thị liên hệ. Vui lòng cấp quyền trong cài đặt ứng dụng.");
            noContactsTextView.setVisibility(View.VISIBLE);
        }
        if (contactsRecyclerView != null) {
            contactsRecyclerView.setVisibility(View.GONE);
        }
        contactList.clear(); // Xóa dữ liệu cũ nếu không có quyền
        if (contactsAdapter != null) {
            contactsAdapter.notifyDataSetChanged();
        }
    }

    private void loadContacts() {
        // Kiểm tra quyền một lần nữa trước khi thực hiện, để đảm bảo
        if (getContext() == null || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permissions not granted when loadContacts() was called. Skipping.");
            onPermissionsDenied(); // Gọi lại hàm này để cập nhật UI
            return;
        }

        executorService.execute(() -> {
            List<ContactInfo> newContactList = new ArrayList<>();
            ContentResolver contentResolver = requireContext().getContentResolver();

            Map<String, ContactInfo> uniqueContacts = new HashMap<>();

            String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            Cursor cursor = null;
            try {
                cursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        null,
                        null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

                if (cursor != null && cursor.getCount() > 0) {
                    int contactIdColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                    int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                    if (contactIdColumnIndex == -1 || nameColumnIndex == -1 || numberColumnIndex == -1) {
                        Log.e(TAG, "Lỗi: Không tìm thấy một hoặc nhiều cột cần thiết trong Cursor.");
                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi truy vấn danh bạ: Thiếu cột dữ liệu.", Toast.LENGTH_LONG).show());
                        return;
                    }

                    while (cursor.moveToNext()) {
                        String id = cursor.getString(contactIdColumnIndex);
                        String name = cursor.getString(nameColumnIndex);
                        String number = cursor.getString(numberColumnIndex);

                        if (name != null && !name.trim().isEmpty() && number != null && !number.trim().isEmpty()) {
                            String normalizedNumber = normalizePhoneNumber(number);

                            if (!uniqueContacts.containsKey(id)) {
                                uniqueContacts.put(id, new ContactInfo(id, name, normalizedNumber));
                            }
                        }
                    }
                    newContactList.addAll(uniqueContacts.values());
                    Collections.sort(newContactList, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Không tìm thấy liên hệ nào.", Toast.LENGTH_SHORT).show());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when reading contacts in ContactsListFragment: " + e.getMessage());
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Quyền truy cập danh bạ bị từ chối.", Toast.LENGTH_LONG).show());
                requireActivity().runOnUiThread(this::onPermissionsDenied);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            requireActivity().runOnUiThread(() -> {
                contactList.clear();
                if (!newContactList.isEmpty()) {
                    contactList.addAll(newContactList);
                    if (noContactsTextView != null) noContactsTextView.setVisibility(View.GONE);
                    if (contactsRecyclerView != null) contactsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    if (noContactsTextView != null) {
                        noContactsTextView.setText("Không có liên hệ nào để hiển thị hoặc quyền truy cập danh bạ bị từ chối.");
                        noContactsTextView.setVisibility(View.VISIBLE);
                    }
                    if (contactsRecyclerView != null) contactsRecyclerView.setVisibility(View.GONE);
                }
                if (contactsAdapter != null) {
                    contactsAdapter.notifyDataSetChanged();
                }
            });
        });
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