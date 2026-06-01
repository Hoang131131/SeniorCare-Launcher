package ntu.edu.seniorcare.sms;

import android.util.Log; // Giữ lại Log cho lỗi (E)
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsInfo {
    private static final String INFO_TAG = "SmsInfo"; // Giữ lại TAG cho các log lỗi

    private String sender;
    private String address;
    private String messageBody;
    private long timestamp;

    public SmsInfo(String sender, String address, String messageBody, long timestamp) {
        this.sender = sender;
        this.address = address;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
        // Log.d(INFO_TAG, "SmsInfo created: Sender='" + sender + "', Address='" + address + "', Body (truncated): '" + (messageBody != null && messageBody.length() > 50 ? messageBody.substring(0, 50) + "..." : messageBody) + "', Timestamp: " + timestamp); // Xóa log này
    }

    public String getSender() {
        return sender;
    }

    public String getAddress() {
        return address;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        String formatted = null;
        try {
            formatted = sdf.format(new Date(timestamp));
        } catch (Exception e) {
            Log.e(INFO_TAG, "Error formatting timestamp " + timestamp + ": " + e.getMessage()); // Giữ lại log lỗi
            formatted = "Lỗi thời gian";
        }
        // Log.d(INFO_TAG, "getFormattedTimestamp for " + sender + " (raw: " + timestamp + "): " + formatted); // Xóa log này
        return formatted;
    }
}