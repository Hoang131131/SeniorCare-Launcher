package ntu.edu.seniorcare;

// Lớp model để lưu trữ thông tin tin nhắn
public class SmsInfo {
    private String sender;
    private String messageBody;
    private long timestamp;

    public SmsInfo(String sender, String messageBody, long timestamp) {
        this.sender = sender;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // You might want to add a method to get formatted timestamp
    public String getFormattedTimestamp() {
        // Implement date formatting as needed
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(null);
        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(null);
        return dateFormat.format(new java.util.Date(timestamp)) + " " + timeFormat.format(new java.util.Date(timestamp));
    }
}