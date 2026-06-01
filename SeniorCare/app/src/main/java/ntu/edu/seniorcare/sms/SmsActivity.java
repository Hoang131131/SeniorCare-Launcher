package ntu.edu.seniorcare.sms;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log; // Giữ lại Log cho lỗi nghiêm trọng (E) và cảnh báo (W)
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ntu.edu.seniorcare.R;

public class SmsActivity extends AppCompatActivity {

    private static final String TAG = "SmsActivity"; // Giữ lại TAG cho các log lỗi
    private static final int PERMISSIONS_REQUEST_CODE = 200;

    private RecyclerView smsRecyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsInfo> latestSmsConversations;
    private TextView noSmsTextView;

    private Map<String, String> contactsMap;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Log.d(TAG, "onCreate called."); // Xóa log này
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
        smsAdapter = new SmsAdapter(this, latestSmsConversations); // Truyền list rỗng này vào adapter
        smsRecyclerView.setAdapter(smsAdapter);
        // Log.d(TAG, "RecyclerView and Adapter initialized."); // Xóa log này

        contactsMap = new HashMap<>();
        executorService = Executors.newSingleThreadExecutor();
        // Log.d(TAG, "Contacts map and ExecutorService initialized."); // Xóa log này

        checkAndRequestPermissions();

        // Log.d(TAG, "onCreate finished."); // Xóa log này
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Log.d(TAG, "onResume called. Initial load handled by onCreate."); // Xóa log này
    }

    private void checkAndRequestPermissions() {
        // Log.d(TAG, "checkAndRequestPermissions called."); // Xóa log này
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
            // Log.d(TAG, "READ_SMS permission not granted."); // Xóa log này
        } else {
            // Log.d(TAG, "READ_SMS permission already granted."); // Xóa log này
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
            // Log.d(TAG, "READ_CONTACTS permission not granted."); // Xóa log này
        } else {
            // Log.d(TAG, "READ_CONTACTS permission already granted."); // Xóa log này
        }

        if (!permissionsNeeded.isEmpty()) {
            // Log.d(TAG, "Requesting " + permissionsNeeded.size() + " permissions."); // Xóa log này
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        } else {
            // Log.d(TAG, "All necessary permissions are granted. Calling loadSmsConversations()."); // Xóa log này
            loadSmsConversations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Log.d(TAG, "onRequestPermissionsResult called for request code: " + requestCode); // Xóa log này
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.e(TAG, "Permission denied for: " + permissions[i]); // Giữ lại log lỗi
                } else {
                    // Log.d(TAG, "Permission granted for: " + permissions[i]); // Xóa log này
                }
            }
            if (allGranted) {
                // Log.d(TAG, "All requested permissions granted. Loading SMS conversations."); // Xóa log này
                loadSmsConversations();
            } else {
                Log.w(TAG, "Not all permissions granted. Displaying error message."); // Giữ lại log cảnh báo
                Toast.makeText(this, "Quyền đọc SMS và Danh bạ bị từ chối. Không thể hiển thị tin nhắn.", Toast.LENGTH_LONG).show();
                if (noSmsTextView != null) {
                    noSmsTextView.setText("Ứng dụng cần quyền đọc SMS và Danh bạ để hiển thị tin nhắn.");
                    noSmsTextView.setVisibility(View.VISIBLE);
                }
                if (smsRecyclerView != null) {
                    smsRecyclerView.setVisibility(View.GONE);
                }
                // latestSmsConversations.clear(); // Xóa log này
                if (smsAdapter != null) {
                    smsAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void loadSmsConversations() {
        // Log.d(TAG, "loadSmsConversations called."); // Xóa log này
        executorService.execute(() -> {
            // Log.d(TAG, "Starting SMS loading on background thread."); // Xóa log này
            loadContactsForLookup();
            // Log.d(TAG, "Contacts loaded. Found " + contactsMap.size() + " contacts."); // Xóa log này

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
                    // Log.d(TAG, "SMS Cursor count: " + cursor.getCount()); // Xóa log này
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

                        int processedSmsCount = 0;
                        do {
                            String address = cursor.getString(addressColumn);
                            String body = cursor.getString(bodyColumn);
                            long date = cursor.getLong(dateColumn);
                            int type = cursor.getInt(typeColumn);

                            if (type == Telephony.Sms.MESSAGE_TYPE_INBOX || type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                                String normalizedAddress = normalizePhoneNumber(address);
                                String senderName = contactsMap.getOrDefault(normalizedAddress, address);

                                if (!latestSmsMap.containsKey(normalizedAddress) || date > latestSmsMap.get(normalizedAddress).getTimestamp()) {
                                    latestSmsMap.put(normalizedAddress, new SmsInfo(senderName, address, body, date));
                                    processedSmsCount++;
                                }
                            }
                        } while (cursor.moveToNext());
                        // Log.d(TAG, "Finished processing SMS cursor. Total processed SMS: " + processedSmsCount + ", Unique conversations: " + latestSmsMap.size()); // Xóa log này
                    } else {
                        // Log.d(TAG, "SMS Cursor is empty (no messages found)."); // Xóa log này
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
                    // Log.d(TAG, "Updating UI on Main Thread. Displaying " + latestSmsConversations.size() + " conversations."); // Xóa log này
                } else {
                    if (noSmsTextView != null) {
                        noSmsTextView.setText("Không có tin nhắn nào.");
                        noSmsTextView.setVisibility(View.VISIBLE);
                    }
                    if (smsRecyclerView != null) smsRecyclerView.setVisibility(View.GONE);
                    // Log.d(TAG, "Updating UI on Main Thread. No messages to display."); // Xóa log này
                }
                if (smsAdapter != null) {
                    smsAdapter.updateSmsList(new ArrayList<>(latestSmsConversations));
                } else {
                    Log.e(TAG, "smsAdapter is null in runOnUiThread. Cannot update list.");
                }
                // Log.d(TAG, "UI update complete. Adapter now shows " + smsAdapter.getItemCount() + " items."); // Xóa log này
            });
            // Log.d(TAG, "loadSmsConversations finished background task."); // Xóa log này
        });
    }

    private void loadContactsForLookup() {
        // Log.d(TAG, "loadContactsForLookup called."); // Xóa log này
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
                // Log.d(TAG, "Contacts Cursor count: " + cursor.getCount()); // Xóa log này
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
                    // Log.d(TAG, "Finished loading contacts. Total unique contacts in map: " + contactsMap.size()); // Xóa log này
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
        // Log.d(TAG, "loadContactsForLookup finished. Contacts loaded count: " + contactsMap.size()); // Xóa log này
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String normalized = phoneNumber.replaceAll("[^\\d+]", "");

        if (normalized.startsWith("0") && normalized.length() > 9) {
            normalized = "+84" + normalized.substring(1);
        } else if (normalized.startsWith("84") && normalized.length() > 9) {
            if (!normalized.startsWith("+")) {
                normalized = "+" + normalized;
            }
        }
        return normalized;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Log.d(TAG, "onDestroy called. Shutting down executor service."); // Xóa log này
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}