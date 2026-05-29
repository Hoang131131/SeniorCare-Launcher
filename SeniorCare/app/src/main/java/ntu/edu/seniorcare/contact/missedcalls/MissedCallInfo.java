package ntu.edu.seniorcare.contact.missedcalls;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MissedCallInfo {
    private String name;
    private String phoneNumber;
    private long callDate; // Timestamp

    public MissedCallInfo(String name, String phoneNumber, long callDate) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.callDate = callDate;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getCallDate() {
        return callDate;
    }

    // Phương thức mới để lấy chỉ thời gian (HH:mm)
    public String getFormattedCallTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(callDate));
    }

    // Phương thức để lấy chỉ ngày (dd/MM/yyyy) - không sử dụng trong item_missed_call nhưng có thể dùng cho header
    public String getFormattedDateOnly() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(callDate));
    }
}