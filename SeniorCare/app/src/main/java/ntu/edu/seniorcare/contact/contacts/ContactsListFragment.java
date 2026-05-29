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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ntu.edu.seniorcare.R;

public class ContactsListFragment extends Fragment {

    private static final String TAG = "ContactsListFragment";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS_FRAGMENT = 200; // Fragment-specific request code

    private RecyclerView contactsRecyclerView;
    private ContactsAdapter contactsAdapter;
    private List<ContactInfo> contactList;

    public ContactsListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts_list, container, false);

        contactsRecyclerView = view.findViewById(R.id.contacts_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        contactList = new ArrayList<>();
        contactsAdapter = new ContactsAdapter(getContext(), contactList);
        contactsRecyclerView.setAdapter(contactsAdapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkPermissionsAndLoadContacts();
    }

    private void checkPermissionsAndLoadContacts() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS_FRAGMENT);
        } else {
            loadContacts();
        }
    }

    private void loadContacts() {
        contactList.clear();
        ContentResolver contentResolver = requireContext().getContentResolver();

        String[] projection = new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        };

        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

        if (cursor != null && cursor.getCount() > 0) {
            int idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int hasPhoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

            if (idColumnIndex == -1 || nameColumnIndex == -1 || hasPhoneNumberColumnIndex == -1) {
                Log.e(TAG, "Lỗi: Không tìm thấy một hoặc nhiều cột cần thiết trong Cursor.");
                Toast.makeText(getContext(), "Lỗi truy vấn danh bạ: Thiếu cột dữ liệu.", Toast.LENGTH_LONG).show();
                cursor.close();
                return;
            }

            while (cursor.moveToNext()) {
                String id = cursor.getString(idColumnIndex);
                String name = cursor.getString(nameColumnIndex);

                String phoneNumber = null;
                if (cursor.getInt(hasPhoneNumberColumnIndex) > 0) {
                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);

                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        int phoneNumberColumnIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (phoneNumberColumnIndex != -1) {
                            phoneNumber = phoneCursor.getString(phoneNumberColumnIndex);
                        } else {
                            Log.e(TAG, "Không tìm thấy cột NUMBER trong Phone Cursor cho liên hệ: " + name);
                        }
                        phoneCursor.close();
                    }
                }

                if (name != null && !name.trim().isEmpty() && phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    contactList.add(new ContactInfo(id, name, phoneNumber));
                }
            }
            cursor.close();
        } else {
            Toast.makeText(getContext(), "Không tìm thấy liên hệ nào.", Toast.LENGTH_SHORT).show();
        }
        contactsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS_FRAGMENT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(getContext(), "Quyền truy cập danh bạ bị từ chối. Không thể hiển thị danh bạ.", Toast.LENGTH_LONG).show();
                // Không đóng activity, chỉ thông báo lỗi
            }
        }
    }
}