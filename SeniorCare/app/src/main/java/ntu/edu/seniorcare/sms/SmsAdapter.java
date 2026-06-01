package ntu.edu.seniorcare.sms;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log; // Giữ lại Log cho lỗi (E)
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.ArrayList; // Thêm import này

import ntu.edu.seniorcare.R;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {

    private static final String ADAPTER_TAG = "SmsAdapter";
    private Context context;
    private List<SmsInfo> smsList; // Danh sách này sẽ được quản lý bởi Adapter

    public SmsAdapter(Context context, List<SmsInfo> smsList) {
        this.context = context;
        // Tạo một bản sao ban đầu để tránh tham chiếu trực tiếp đến list của Activity
        this.smsList = new ArrayList<>(smsList);
        // Log.d(ADAPTER_TAG, "SmsAdapter initialized with " + this.smsList.size() + " items."); // Xóa log này
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Log.d(ADAPTER_TAG, "onCreateViewHolder: creating new ViewHolder."); // Xóa log này
        View view = LayoutInflater.from(context).inflate(R.layout.item_sms, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        if (position < 0 || position >= smsList.size()) {
            Log.e(ADAPTER_TAG, "onBindViewHolder: Invalid position: " + position + ". smsList size: " + smsList.size()); // Giữ lại log lỗi
            return;
        }
        SmsInfo sms = smsList.get(position);

        String senderText = sms.getSender();
        if (senderText == null || senderText.isEmpty() || senderText.equals(sms.getAddress())) {
            senderText = "Người gửi: " + sms.getAddress();
        } else {
            senderText = "Người gửi: " + senderText;
        }

        // Log.d(ADAPTER_TAG, "onBindViewHolder: Binding position " + position + ". Sender: " + senderText + ", Body: " + (sms.getMessageBody() != null && sms.getMessageBody().length() > 50 ? sms.getMessageBody().substring(0, 50) + "..." : sms.getMessageBody()) + ", Timestamp: " + sms.getFormattedTimestamp()); // Xóa log này

        holder.textSender.setText(senderText);
        holder.textMessageBody.setText(sms.getMessageBody());
        holder.textTimestamp.setText(sms.getFormattedTimestamp());

        holder.itemView.setOnClickListener(v -> {
            // Log.d(ADAPTER_TAG, "Item clicked at position: " + position + ". Opening SMS for address: " + sms.getAddress()); // Xóa log này
            try {
                Uri smsUri = Uri.parse("smsto:" + sms.getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
                if (!(context instanceof android.app.Activity)) { // Thêm flag nếu context không phải là Activity
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(ADAPTER_TAG, "Failed to open SMS app for address " + sms.getAddress() + ". Error: " + e.getMessage()); // Giữ lại log lỗi
                Toast.makeText(context, "Không thể mở ứng dụng tin nhắn. Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = smsList != null ? smsList.size() : 0; // Kiểm tra null trước khi lấy size
        // Log.d(ADAPTER_TAG, "getItemCount called. Current size: " + count); // Xóa log này
        return count;
    }

    public void updateSmsList(List<SmsInfo> newSmsList) {
        // Log.d(ADAPTER_TAG, "updateSmsList called. New list size: " + (newSmsList != null ? newSmsList.size() : "null") + ". Current adapter list size before clear: " + (this.smsList != null ? this.smsList.size() : "null")); // Xóa log này
        if (this.smsList == null) {
            this.smsList = new ArrayList<>();
        }
        this.smsList.clear(); // Xóa sạch danh sách cũ
        if (newSmsList != null) {
            this.smsList.addAll(newSmsList); // Thêm các phần tử từ danh sách mới
        }
        notifyDataSetChanged();
        // Log.d(ADAPTER_TAG, "notifyDataSetChanged called. Adapter now has " + this.smsList.size() + " items."); // Xóa log này
    }

    public static class SmsViewHolder extends RecyclerView.ViewHolder {
        TextView textSender;
        TextView textMessageBody;
        TextView textTimestamp;

        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.text_sender);
            textMessageBody = itemView.findViewById(R.id.text_message_body);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);

            if (textSender == null || textMessageBody == null || textTimestamp == null) {
                Log.e(ADAPTER_TAG, "SmsViewHolder: One or more TextViews not found! Check item_sms.xml for IDs."); // Giữ lại log lỗi
            } else {
                // Log.d(ADAPTER_TAG, "SmsViewHolder: All TextViews found."); // Xóa log này
            }
        }
    }
}