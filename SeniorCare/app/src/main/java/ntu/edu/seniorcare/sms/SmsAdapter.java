package ntu.edu.seniorcare.sms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ntu.edu.seniorcare.R;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {

    private List<SmsInfo> smsList;

    public SmsAdapter(List<SmsInfo> smsList) {
        this.smsList = smsList;
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sms, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsInfo sms = smsList.get(position);
        holder.textSender.setText(sms.getSender());
        holder.textMessageBody.setText(sms.getMessageBody());
        holder.textTimestamp.setText(sms.getFormattedTimestamp());
    }

    @Override
    public int getItemCount() {
        return smsList.size();
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

    public void updateSmsList(List<SmsInfo> newSmsList) {
        this.smsList = newSmsList;
        notifyDataSetChanged();
    }
}