package ntu.edu.seniorcare;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log; // Thêm Log để gỡ lỗi
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_CALL_PHONE = 101;

    private RecyclerView contactsRecyclerView;
    private ContactsAdapter contactsAdapter;
    private List<ContactInfo> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contactsRecyclerView = findViewById(R.id.contacts_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        contactList = new ArrayList<>();
        // Khởi tạo ContactsAdapter với Constructor mới (không có photoUri)
        contactsAdapter = new ContactsAdapter(this, contactList);
        contactsRecyclerView.setAdapter(contactsAdapter);

        checkPermissionsAndLoadContacts();
    }

    private void checkPermissionsAndLoadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            loadContacts();
        }
    }

    private void loadContacts() {
        contactList.clear();
        ContentResolver contentResolver = getContentResolver();

        // Định nghĩa projection: chỉ các cột cần thiết
        String[] projection = new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
                // Đã xóa: ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        };

        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection, // Sử dụng projection đã định nghĩa
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

        if (cursor != null && cursor.getCount() > 0) {
            // Lấy chỉ mục của các cột MỘT LẦN duy nhất
            int idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int hasPhoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

            // Kiểm tra xem các chỉ mục có hợp lệ hay không (rất quan trọng)
            if (idColumnIndex == -1) {
                Log.e("ContactsActivity", "Không tìm thấy cột _ID trong Cursor.");
                Toast.makeText(this, "Lỗi truy vấn danh bạ: Không tìm thấy cột ID.", Toast.LENGTH_LONG).show();
                cursor.close();
                return;
            }
            if (nameColumnIndex == -1) {
                Log.e("ContactsActivity", "Không tìm thấy cột DISPLAY_NAME trong Cursor.");
                Toast.makeText(this, "Lỗi truy vấn danh bạ: Không tìm thấy cột Tên.", Toast.LENGTH_LONG).show();
                cursor.close();
                return;
            }
            if (hasPhoneNumberColumnIndex == -1) {
                Log.e("ContactsActivity", "Không tìm thấy cột HAS_PHONE_NUMBER trong Cursor.");
                Toast.makeText(this, "Lỗi truy vấn danh bạ: Không tìm thấy cột số điện thoại.", Toast.LENGTH_LONG).show();
                cursor.close();
                return;
            }


            while (cursor.moveToNext()) {
                String id = cursor.getString(idColumnIndex);
                String name = cursor.getString(nameColumnIndex);

                // Đã xóa: Uri photoUri = null; // Luôn đặt là null vì không sử dụng

                // Lấy số điện thoại
                String phoneNumber = null;
                // Chỉ truy vấn số điện thoại nếu contact có HAS_PHONE_NUMBER > 0
                if (cursor.getInt(hasPhoneNumberColumnIndex) > 0) {
                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);

                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        int phoneNumberColumnIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (phoneNumberColumnIndex != -1) { // Kiểm tra chỉ mục hợp lệ
                            phoneNumber = phoneCursor.getString(phoneNumberColumnIndex);
                        } else {
                            Log.e("ContactsActivity", "Không tìm thấy cột NUMBER trong Phone Cursor cho liên hệ: " + name);
                        }
                        phoneCursor.close();
                    }
                }

                // Thêm liên hệ vào danh sách nếu có tên và số điện thoại
                if (name != null && !name.trim().isEmpty() && phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    contactList.add(new ContactInfo(id, name, phoneNumber)); // Constructor đã sửa
                }
            }
            cursor.close();
        } else {
            Toast.makeText(this, "Không tìm thấy liên hệ nào.", Toast.LENGTH_SHORT).show();
        }
        contactsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, "Quyền truy cập danh bạ bị từ chối. Không thể hiển thị danh bạ.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}