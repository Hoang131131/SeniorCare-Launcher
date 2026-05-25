package ntu.edu.seniorcare;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private RecyclerView smsRecyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsInfo> smsList;
    private Map<String, String> contactNumbersMap; // Map để lưu trữ số điện thoại từ danh bạ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        smsRecyclerView = findViewById(R.id.sms_recycler_view);
        smsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        smsList = new ArrayList<>();
        smsAdapter = new SmsAdapter(smsList);
        smsRecyclerView.setAdapter(smsAdapter);

        checkPermissionsAndLoadSms();
    }

    private void checkPermissionsAndLoadSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_CODE);
        } else {
            loadContacts();
            loadSmsMessages();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
                loadSmsMessages();
            } else {
                Toast.makeText(this, "Quyền đọc tin nhắn và danh bạ bị từ chối. Không thể hiển thị tin nhắn.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadContacts() {
        contactNumbersMap = new HashMap<>();
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    // Chuẩn hóa số điện thoại để so sánh (xóa ký tự không phải số)
                    String normalizedNumber = number.replaceAll("[^\\d+]", "");
                    if (!normalizedNumber.isEmpty()) {
                        contactNumbersMap.put(normalizedNumber, name);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void loadSmsMessages() {
        if (contactNumbersMap == null || contactNumbersMap.isEmpty()) {
            Toast.makeText(this, "Không thể tải danh bạ hoặc không có liên hệ nào. Chỉ hiển thị tin nhắn từ các số đã lưu.", Toast.LENGTH_LONG).show();
            // Nếu không có danh bạ, có thể chọn hiển thị tất cả hoặc không hiển thị gì.
            // Hiện tại, chúng ta sẽ không thêm tin nhắn nào nếu không có danh bạ để lọc.
            smsAdapter.updateSmsList(new ArrayList<>()); // Clear existing list
            return;
        }

        List<SmsInfo> fetchedSmsList = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Uri uri = Uri.parse("content://sms/inbox"); // Chỉ đọc tin nhắn đến (inbox)

        // Các cột cần lấy từ tin nhắn
        String[] projection = new String[]{
                "_id",      // ID tin nhắn
                "address",  // Số điện thoại người gửi
                "body",     // Nội dung tin nhắn
                "date"      // Thời gian gửi (milliseconds since epoch)
        };

        Cursor cursor = cr.query(uri, projection, null, null, "date DESC"); // Sắp xếp theo ngày giảm dần

        if (cursor != null) {
            try {
                int addressCol = cursor.getColumnIndexOrThrow("address");
                int bodyCol = cursor.getColumnIndexOrThrow("body");
                int dateCol = cursor.getColumnIndexOrThrow("date");

                while (cursor.moveToNext()) {
                    String address = cursor.getString(addressCol);
                    String body = cursor.getString(bodyCol);
                    long date = cursor.getLong(dateCol);

                    // Chuẩn hóa số điện thoại của người gửi để so sánh với danh bạ
                    String normalizedAddress = address.replaceAll("[^\\d+]", "");

                    // Lọc tin nhắn: chỉ hiển thị nếu số người gửi có trong danh bạ
                    if (contactNumbersMap.containsKey(normalizedAddress)) {
                        String senderName = contactNumbersMap.get(normalizedAddress);
                        if (senderName == null || senderName.isEmpty()) { // Fallback to number if name is empty
                            senderName = address;
                        }
                        fetchedSmsList.add(new SmsInfo(senderName, body, date));
                    }
                }
            } finally {
                cursor.close();
            }
        }

        // Cập nhật RecyclerView với danh sách tin nhắn đã lọc
        smsList.clear();
        smsList.addAll(fetchedSmsList);
        smsAdapter.notifyDataSetChanged();
    }
}