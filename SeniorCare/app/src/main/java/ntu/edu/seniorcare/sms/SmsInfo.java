package ntu.edu.seniorcare.sms; // Đảm bảo package đúng

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsInfo {
    private String sender;      // Tên người gửi (hoặc số nếu không có trong danh bạ)
    private String address;     // Số điện thoại thực tế
    private String messageBody;
    private long timestamp;

    public SmsInfo(String sender, String address, String messageBody, long timestamp) {
        this.sender = sender;
        this.address = address;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
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
        // Định dạng "10:30 AM - 15/05/2024"
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}