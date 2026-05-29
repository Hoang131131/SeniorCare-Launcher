package ntu.edu.seniorcare.contact.missedcalls;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import ntu.edu.seniorcare.R; // Đảm bảo import R đúng nếu package khác

public class MissedCallAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<MissedCallGroupItem> items; // Danh sách chứa cả header và call items

    public MissedCallAdapter(Context context, List<MissedCallGroupItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == MissedCallGroupItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_missed_call_header, parent, false);
            return new HeaderViewHolder(view);
        } else { // TYPE_CALL_ITEM
            View view = inflater.inflate(R.layout.item_missed_call, parent, false);
            return new MissedCallViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == MissedCallGroupItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            MissedCallHeaderItem headerItem = (MissedCallHeaderItem) items.get(position);
            headerHolder.dateTextView.setText(headerItem.getDateHeader());
        } else { // TYPE_CALL_ITEM
            MissedCallViewHolder callHolder = (MissedCallViewHolder) holder;
            MissedCallDetailItem detailItem = (MissedCallDetailItem) items.get(position);
            MissedCallInfo missedCall = detailItem.getMissedCallInfo();

            callHolder.callerName.setText(missedCall.getName());
            callHolder.phoneNumber.setText(missedCall.getPhoneNumber());
            // Chỉ hiển thị giờ gọi, bỏ phần ngày
            callHolder.callTime.setText(missedCall.getFormattedCallTime());

            callHolder.btnCallBack.setOnClickListener(v -> {
                String phoneNumber = missedCall.getPhoneNumber();
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:" + phoneNumber));
                        context.startActivity(callIntent);
                    } else {
                        Toast.makeText(context, "Ứng dụng cần quyền CALL_PHONE để thực hiện cuộc gọi. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "Không có số điện thoại để gọi lại.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder cho item cuộc gọi nhỡ
    public static class MissedCallViewHolder extends RecyclerView.ViewHolder {
        TextView callerName;
        TextView phoneNumber;
        TextView callTime; // Chỉ giữ TextView cho giờ
        MaterialButton btnCallBack;

        public MissedCallViewHolder(@NonNull View itemView) {
            super(itemView);
            callerName = itemView.findViewById(R.id.missed_caller_name);
            phoneNumber = itemView.findViewById(R.id.missed_phone_number);
            callTime = itemView.findViewById(R.id.missed_call_time); // Ánh xạ TextView giờ
            btnCallBack = itemView.findViewById(R.id.btn_call_back);
        }
    }

    // ViewHolder cho header ngày
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.text_header_date);
        }
    }
}