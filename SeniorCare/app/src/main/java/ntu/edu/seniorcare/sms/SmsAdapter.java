package ntu.edu.seniorcare.sms; // Đảm bảo package đúng

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ntu.edu.seniorcare.R; // Đảm bảo import R đúng nếu package khác

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {

    private Context context;
    private List<SmsInfo> smsList;

    public SmsAdapter(Context context, List<SmsInfo> smsList) {
        this.context = context;
        this.smsList = smsList;
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sms, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsInfo sms = smsList.get(position);

        // Hiển thị tên người gửi hoặc số điện thoại nếu không có tên
        String senderText = sms.getSender();
        if (senderText == null || senderText.isEmpty() || senderText.equals(sms.getAddress())) {
            senderText = "Người gửi: " + sms.getAddress();
        } else {
            senderText = "Người gửi: " + senderText;
        }
        holder.textSender.setText(senderText);
        holder.textMessageBody.setText(sms.getMessageBody());
        holder.textTimestamp.setText(sms.getFormattedTimestamp());

        // Xử lý sự kiện click vào item
        holder.itemView.setOnClickListener(v -> {
            try {
                // Mở ứng dụng tin nhắn mặc định của hệ thống với cuộc hội thoại cụ thể
                // ACTION_SENDTO sẽ mở một activity gửi tin nhắn mới (chưa có nội dung)
                // ACTION_VIEW sẽ mở cuộc hội thoại có sẵn (ưu tiên hơn)

                Uri smsUri = Uri.parse("smsto:" + sms.getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
                // Bạn có thể thêm flag để đảm bảo ứng dụng của bạn vẫn ở phía sau.
                // Tuy nhiên, ACTION_VIEW mặc định thường xử lý khá tốt việc này.
                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Có thể thử nếu gặp vấn đề
                context.startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(context, "Không thể mở ứng dụng tin nhắn. Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

    public void updateSmsList(List<SmsInfo> newSmsList) {
        this.smsList.clear();
        this.smsList.addAll(newSmsList);
        notifyDataSetChanged();
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
        }
    }
}