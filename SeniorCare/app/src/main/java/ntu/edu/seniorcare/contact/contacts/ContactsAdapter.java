package ntu.edu.seniorcare.contact.contacts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ntu.edu.seniorcare.R;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private final Context context;
    private final List<ContactInfo> contactList;

    public ContactsAdapter(Context context, List<ContactInfo> contactList) {
        this.context = context;
        this.contactList = contactList;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactInfo contact = contactList.get(position);
        holder.contactName.setText(contact.getName());
        holder.contactPhone.setText(contact.getPhoneNumber());

        // Button: Gọi thường (sẽ kích hoạt bộ chọn ứng dụng)
        holder.btnCallPhone.setOnClickListener(v -> {
            String phoneNumber = contact.getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                // Sử dụng ACTION_DIAL để mở ứng dụng quay số và kích hoạt bộ chọn ứng dụng
                // Không cần kiểm tra quyền CALL_PHONE với ACTION_DIAL
                Intent dialIntent = new Intent(Intent.ACTION_CALL);
                dialIntent.setData(Uri.parse("tel:" + phoneNumber.replaceAll("[^\\d+]", ""))); // Đảm bảo số điện thoại sạch

                // Kiểm tra xem có ứng dụng nào có thể xử lý Intent này không
                PackageManager packageManager = context.getPackageManager();
                if (dialIntent.resolveActivity(packageManager) != null) {
                    // Tạo một Chooser Intent để đảm bảo người dùng được hỏi
                    // Đây là cách tốt nhất để hiển thị bộ chọn ứng dụng
                    Intent chooserIntent = Intent.createChooser(dialIntent, "Gọi bằng");
                    try {
                        context.startActivity(chooserIntent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(context, "Không tìm thấy ứng dụng nào để thực hiện cuộc gọi.", Toast.LENGTH_SHORT).show();
                        Log.e("ContactsAdapter", "No app found to handle ACTION_CALL: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(context, "Không tìm thấy ứng dụng nào để thực hiện cuộc gọi.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Số điện thoại không hợp lệ.", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }


    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView contactName;
        TextView contactPhone;
        Button btnCallPhone;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
            btnCallPhone = itemView.findViewById(R.id.btn_call_phone);
        }
    }
}