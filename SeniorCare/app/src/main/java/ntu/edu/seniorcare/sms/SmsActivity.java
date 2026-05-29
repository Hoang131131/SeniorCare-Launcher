package ntu.edu.seniorcare.sms; // Đảm bảo package đúng

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ntu.edu.seniorcare.R; // Đảm bảo import R đúng nếu package khác

public class SmsActivity extends AppCompatActivity {

    private static final String TAG = "SmsActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 200; // Request code cho SMS và Contacts

    private RecyclerView smsRecyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsInfo> latestSmsConversations; // Danh sách các tin nhắn gần nhất cho mỗi cuộc hội thoại
    private TextView noSmsTextView; // Thêm TextView để hiển thị khi không có SMS

    private Map<String, String> contactsMap; // key: normalized number, value: contact name
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        smsRecyclerView = findViewById(R.id.sms_recycler_view);
        noSmsTextView = findViewById(R.id.no_sms_text); // Cần thêm TextView này vào activity_sms.xml
        smsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        latestSmsConversations = new ArrayList<>();
        smsAdapter = new SmsAdapter(this, latestSmsConversations);
        smsRecyclerView.setAdapter(smsAdapter);

        contactsMap = new HashMap<>();
        executorService = Executors.newSingleThreadExecutor();

        // Kiểm tra và yêu cầu quyền
        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại tin nhắn mỗi khi Activity resume (để cập nhật tin nhắn mới)
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        } else {
            loadSmsConversations();
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
                loadSmsConversations();
            } else {
                Toast.makeText(this, "Quyền đọc SMS và Danh bạ bị từ chối. Không thể hiển thị tin nhắn.", Toast.LENGTH_LONG).show();
                noSmsTextView.setText("Ứng dụng cần quyền đọc SMS và Danh bạ để hiển thị tin nhắn.");
                noSmsTextView.setVisibility(View.VISIBLE);
                smsRecyclerView.setVisibility(View.GONE);
                latestSmsConversations.clear();
                smsAdapter.notifyDataSetChanged();
            }
        }
    }

    private void loadSmsConversations() {
        executorService.execute(() -> {
            // 1. Tải danh bạ trước để có tên liên hệ
            loadContactsForLookup();

            // 2. Tải tất cả tin nhắn và nhóm lại
            Map<String, SmsInfo> latestSmsMap = new HashMap<>(); // key: normalized address, value: latest SmsInfo
            ContentResolver cr = getContentResolver();

            Uri smsUri = Telephony.Sms.CONTENT_URI; // Uri cho tất cả tin nhắn (inbox, sent, draft)
            String[] projection = new String[]{
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE // Để phân biệt tin nhắn đến/đi
            };
            String selection = null; // Không lọc, lấy tất cả

            Cursor cursor = null;
            try {
                cursor = cr.query(smsUri, projection, selection, null, Telephony.Sms.DATE + " DESC"); // Sắp xếp theo ngày giảm dần

                if (cursor != null && cursor.moveToFirst()) {
                    int addressColumn = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    int bodyColumn = cursor.getColumnIndex(Telephony.Sms.BODY);
                    int dateColumn = cursor.getColumnIndex(Telephony.Sms.DATE);
                    int typeColumn = cursor.getColumnIndex(Telephony.Sms.TYPE);

                    do {
                        String address = cursor.getString(addressColumn);
                        String body = cursor.getString(bodyColumn);
                        long date = cursor.getLong(dateColumn);
                        int type = cursor.getInt(typeColumn);

                        // Chỉ lấy tin nhắn từ INBOX hoặc SENT
                        if (type == Telephony.Sms.MESSAGE_TYPE_INBOX || type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                            String normalizedAddress = normalizePhoneNumber(address);

                            // Nếu số này không có trong danh bạ, chúng ta có thể bỏ qua hoặc hiển thị số.
                            // Với yêu cầu của bạn, chỉ hiện từ danh bạ, nhưng tôi sẽ cho phép cả số không có tên để tránh mất tin nhắn.
                            String senderName = contactsMap.getOrDefault(normalizedAddress, address);

                            // Chỉ lưu tin nhắn mới nhất cho mỗi cuộc hội thoại
                            if (!latestSmsMap.containsKey(normalizedAddress) || date > latestSmsMap.get(normalizedAddress).getTimestamp()) {
                                latestSmsMap.put(normalizedAddress, new SmsInfo(senderName, address, body, date));
                            }
                        }
                    } while (cursor.moveToNext());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Lỗi quyền khi đọc SMS: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Quyền đọc SMS bị từ chối.", Toast.LENGTH_LONG).show());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Cập nhật UI trên Main Thread
            runOnUiThread(() -> {
                latestSmsConversations.clear();
                if (!latestSmsMap.isEmpty()) {
                    // Sắp xếp các cuộc hội thoại theo thời gian tin nhắn gần nhất
                    List<SmsInfo> sortedSms = new ArrayList<>(latestSmsMap.values());
                    Collections.sort(sortedSms, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp())); // Mới nhất lên đầu
                    latestSmsConversations.addAll(sortedSms);
                    noSmsTextView.setVisibility(View.GONE);
                    smsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    noSmsTextView.setText("Không có tin nhắn nào.");
                    noSmsTextView.setVisibility(View.VISIBLE);
                    smsRecyclerView.setVisibility(View.GONE);
                }
                smsAdapter.updateSmsList(latestSmsConversations);
            });
        });
    }

    private void loadContactsForLookup() {
        contactsMap.clear();
        ContentResolver contentResolver = getContentResolver();

        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null);

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
                } else {
                    Log.e(TAG, "Không tìm thấy cột NUMBER hoặc DISPLAY_NAME trong Cursor danh bạ.");
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Lỗi quyền khi đọc danh bạ: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Chuẩn hóa số điện thoại. Loại bỏ các ký tự không phải số và cố gắng thống nhất định dạng.
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String normalized = phoneNumber.replaceAll("[^\\d+]", "");

        if (normalized.startsWith("0") && normalized.length() > 9) {
            normalized = "+84" + normalized.substring(1);
        } else if (normalized.startsWith("84") && normalized.length() > 9) {
            normalized = "+" + normalized;
        }
        return normalized;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}