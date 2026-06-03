package ntu.edu.seniorcare.sms;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet; // Dùng để lưu trữ các số điện thoại từ danh bạ để tra cứu nhanh hơn
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set; // Dùng để lưu trữ các số điện thoại từ danh bạ để tra cứu nhanh hơn
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ntu.edu.seniorcare.R;

public class SmsActivity extends AppCompatActivity {

    private static final String TAG = "SmsActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 200;

    private RecyclerView smsRecyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsInfo> latestSmsConversations;
    private TextView noSmsTextView;

    // Thay vì Map, sử dụng Set để lưu trữ các số điện thoại đã chuẩn hóa từ danh bạ
    // Việc này hiệu quả hơn khi chỉ cần kiểm tra sự tồn tại
    private Set<String> contactPhoneNumbers;
    private Map<String, String> contactNamesMap; // Map để tra cứu tên từ số điện thoại
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        smsRecyclerView = findViewById(R.id.sms_recycler_view);
        noSmsTextView = findViewById(R.id.no_sms_text);

        if (smsRecyclerView == null) {
            Log.e(TAG, "smsRecyclerView is null! Check activity_sms.xml layout file for id @+id/sms_recycler_view.");
            Toast.makeText(this, "Lỗi: Không tìm thấy RecyclerView. Kiểm tra layout.", Toast.LENGTH_LONG).show();
            return;
        }
        if (noSmsTextView == null) {
            Log.e(TAG, "noSmsTextView is null! Check activity_sms.xml layout file for id @+id/no_sms_text.");
        }

        smsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        latestSmsConversations = new ArrayList<>();
        smsAdapter = new SmsAdapter(this, latestSmsConversations);
        smsRecyclerView.setAdapter(smsAdapter);

        // Khởi tạo Set và Map mới
        contactPhoneNumbers = new HashSet<>();
        contactNamesMap = new HashMap<>();
        executorService = Executors.newSingleThreadExecutor();

        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Để đảm bảo tin nhắn được tải lại khi quay lại activity, đặc biệt sau khi cấp quyền
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadSmsConversations();
        }
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
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.e(TAG, "Permission denied for: " + permissions[i]);
                }
            }
            if (allGranted) {
                loadSmsConversations();
            } else {
                Log.w(TAG, "Not all permissions granted. Displaying error message.");
                Toast.makeText(this, "Quyền đọc SMS và Danh bạ bị từ chối. Không thể hiển thị tin nhắn.", Toast.LENGTH_LONG).show();
                if (noSmsTextView != null) {
                    noSmsTextView.setText("Ứng dụng cần quyền đọc SMS và Danh bạ để hiển thị tin nhắn.");
                    noSmsTextView.setVisibility(View.VISIBLE);
                }
                if (smsRecyclerView != null) {
                    smsRecyclerView.setVisibility(View.GONE);
                }
                if (smsAdapter != null) {
                    smsAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void loadSmsConversations() {
        executorService.execute(() -> {
            loadContactsForLookup(); // Tải danh bạ trước

            if (contactPhoneNumbers.isEmpty() && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Contacts loaded, but contactPhoneNumbers is empty. This might mean no contacts or an issue during loading.");
            }

            Map<String, SmsInfo> latestSmsMap = new HashMap<>();
            ContentResolver cr = getContentResolver();

            Uri smsUri = Telephony.Sms.CONTENT_URI;
            String[] projection = new String[]{
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
            };
            String selection = null;

            Cursor cursor = null;
            try {
                cursor = cr.query(smsUri, projection, selection, null, Telephony.Sms.DATE + " DESC");

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int addressColumn = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                        int bodyColumn = cursor.getColumnIndex(Telephony.Sms.BODY);
                        int dateColumn = cursor.getColumnIndex(Telephony.Sms.DATE);
                        int typeColumn = cursor.getColumnIndex(Telephony.Sms.TYPE);

                        if (addressColumn == -1 || bodyColumn == -1 || dateColumn == -1 || typeColumn == -1) {
                            Log.e(TAG, "One or more SMS columns not found. Check Telephony.Sms constants. Address: " + addressColumn + ", Body: " + bodyColumn + ", Date: " + dateColumn + ", Type: " + typeColumn);
                            runOnUiThread(() -> Toast.makeText(SmsActivity.this, "Lỗi: Không tìm thấy cột dữ liệu SMS.", Toast.LENGTH_LONG).show());
                            return;
                        }

                        do {
                            String address = cursor.getString(addressColumn);
                            String body = cursor.getString(bodyColumn);
                            long date = cursor.getLong(dateColumn);
                            int type = cursor.getInt(typeColumn);

                            if (type == Telephony.Sms.MESSAGE_TYPE_INBOX || type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                                String normalizedAddress = normalizePhoneNumber(address);

                                // Chỉ xử lý tin nhắn nếu số đã chuẩn hóa có trong danh bạ đã tải
                                if (contactPhoneNumbers.contains(normalizedAddress)) {
                                    String senderName = contactNamesMap.getOrDefault(normalizedAddress, address); // Lấy tên từ contactNamesMap
                                    if (!latestSmsMap.containsKey(normalizedAddress) || date > latestSmsMap.get(normalizedAddress).getTimestamp()) {
                                        latestSmsMap.put(normalizedAddress, new SmsInfo(senderName, address, body, date));
                                    }
                                } else {
                                    // Log.d(TAG, "Tin nhắn từ số " + address + " không có trong danh bạ. Bỏ qua.");
                                }
                            }
                        } while (cursor.moveToNext());
                    }
                } else {
                    Log.e(TAG, "SMS Cursor is null. No SMS data could be retrieved. Check permissions or device SMS.");
                    runOnUiThread(() -> Toast.makeText(SmsActivity.this, "Lỗi: Không thể truy xuất dữ liệu SMS.", Toast.LENGTH_LONG).show());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when reading SMS: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(SmsActivity.this, "Quyền đọc SMS bị từ chối.", Toast.LENGTH_LONG).show();
                    if (noSmsTextView != null) {
                        noSmsTextView.setText("Ứng dụng cần quyền đọc SMS để hiển thị tin nhắn. Vui lòng cấp quyền.");
                        noSmsTextView.setVisibility(View.VISIBLE);
                    }
                    if (smsRecyclerView != null) {
                        smsRecyclerView.setVisibility(View.GONE);
                    }
                    if (smsAdapter != null) {
                        smsAdapter.updateSmsList(new ArrayList<>());
                    }
                });
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            runOnUiThread(() -> {
                latestSmsConversations.clear();
                if (!latestSmsMap.isEmpty()) {
                    List<SmsInfo> sortedSms = new ArrayList<>(latestSmsMap.values());
                    Collections.sort(sortedSms, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    latestSmsConversations.addAll(sortedSms);
                    if (noSmsTextView != null) noSmsTextView.setVisibility(View.GONE);
                    if (smsRecyclerView != null) smsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    if (noSmsTextView != null) {
                        // Cập nhật thông báo nếu không có tin nhắn từ danh bạ
                        noSmsTextView.setText("Không có tin nhắn nào từ các liên hệ trong danh bạ.");
                        noSmsTextView.setVisibility(View.VISIBLE);
                    }
                    if (smsRecyclerView != null) smsRecyclerView.setVisibility(View.GONE);
                }
                if (smsAdapter != null) {
                    smsAdapter.updateSmsList(new ArrayList<>(latestSmsConversations));
                } else {
                    Log.e(TAG, "smsAdapter is null in runOnUiThread. Cannot update list.");
                }
            });
        });
    }

    private void loadContactsForLookup() {
        // Clear cả hai cấu trúc dữ liệu trước khi tải lại
        contactPhoneNumbers.clear();
        contactNamesMap.clear();

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
                            String normalizedNumber = normalizePhoneNumber(number);
                            contactPhoneNumbers.add(normalizedNumber); // Thêm số điện thoại đã chuẩn hóa vào Set
                            contactNamesMap.put(normalizedNumber, name); // Lưu tên tương ứng vào Map
                        }
                    }
                } else {
                    Log.e(TAG, "Không tìm thấy cột NUMBER hoặc DISPLAY_NAME trong Cursor danh bạ. numberColumnIndex: " + numberColumnIndex + ", nameColumnIndex: " + nameColumnIndex);
                }
            } else {
                Log.e(TAG, "Contacts Cursor is null. No contacts could be retrieved.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Lỗi quyền khi đọc danh bạ: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(SmsActivity.this, "Quyền đọc danh bạ bị từ chối.", Toast.LENGTH_LONG).show());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String normalized = phoneNumber.replaceAll("[^\\d+]", "");

        // Xử lý các tiền tố quốc tế và nội địa
        if (normalized.startsWith("0")) {
            normalized = "+84" + normalized.substring(1);
        } else if (normalized.startsWith("84")) {
            if (!normalized.startsWith("+")) {
                normalized = "+" + normalized;
            }
        } else if (!normalized.startsWith("+") && normalized.length() > 7) {

        }
        return normalized;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}