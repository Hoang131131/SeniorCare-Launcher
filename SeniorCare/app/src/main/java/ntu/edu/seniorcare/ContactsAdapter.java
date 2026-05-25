package ntu.edu.seniorcare;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.List;

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

        // Load contact photo
        if (contact.getPhotoUri() != null) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(contact.getPhotoUri());
                Drawable photo = Drawable.createFromStream(inputStream, contact.getPhotoUri().toString());
                holder.contactPhoto.setImageDrawable(photo);
            } catch (Exception e) {
                // Fallback to default icon if photo cannot be loaded
                holder.contactPhoto.setImageResource(R.drawable.ic_contacts);
                e.printStackTrace();
            }
        } else {
            holder.contactPhoto.setImageResource(R.drawable.ic_contacts);
        }

        // Button: Gọi thường
        holder.btnCallPhone.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
                if (callIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(callIntent);
                } else {
                    Toast.makeText(context, "Không có ứng dụng gọi điện nào được tìm thấy.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Cần cấp quyền CALL_PHONE để thực hiện cuộc gọi.", Toast.LENGTH_LONG).show();
                // Trong một ứng dụng thực tế, bạn sẽ cần yêu cầu quyền ở Activity
                // hoặc sử dụng EventBus để gửi sự kiện yêu cầu quyền về Activity.
            }
        });

        // Button: Gọi Zalo (Đây là một chức năng phức tạp và có thể không ổn định)
        holder.btnCallZalo.setOnClickListener(v -> {
            String phoneNumber = contact.getPhoneNumber();
            // Xóa mọi ký tự không phải số khỏi số điện thoại
            phoneNumber = phoneNumber.replaceAll("[^0-9]", "");

            // Cố gắng mở Zalo chat/call theo số điện thoại
            // Lưu ý: Zalo không cung cấp API chính thức cho việc này.
            // Các deep link sau có thể hoạt động hoặc không tùy thuộc vào phiên bản Zalo.
            // Zalo ID là cách đáng tin cậy hơn, nhưng chúng ta chỉ có số điện thoại ở đây.

            Uri uri = Uri.parse("zalo://call?number=" + phoneNumber); // Thử gọi
            Intent zaloIntent = new Intent(Intent.ACTION_VIEW, uri);
            zaloIntent.setPackage("com.zing.zalo"); // Đảm bảo chỉ mở Zalo

            if (isPackageInstalled("com.zing.zalo", context.getPackageManager())) {
                if (zaloIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(zaloIntent);
                } else {
                    // Nếu deep link call không hoạt động, thử deep link chat
                    Uri chatUri = Uri.parse("zalo://chat?phone=" + phoneNumber);
                    Intent zaloChatIntent = new Intent(Intent.ACTION_VIEW, chatUri);
                    zaloChatIntent.setPackage("com.zing.zalo");
                    if (zaloChatIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(zaloChatIntent);
                    } else {
                        Toast.makeText(context, "Không thể mở Zalo chat/gọi với số này. Hãy thử kiểm tra lại số Zalo.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(context, "Ứng dụng Zalo chưa được cài đặt.", Toast.LENGTH_SHORT).show();
                // Có thể mở Google Play Store để cài đặt Zalo
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.zing.zalo")));
                } catch (android.content.ActivityNotFoundException anfe) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.zing.zalo")));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView contactPhoto;
        TextView contactName;
        TextView contactPhone;
        Button btnCallPhone;
        Button btnCallZalo;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
            btnCallPhone = itemView.findViewById(R.id.btn_call_phone);
            btnCallZalo = itemView.findViewById(R.id.btn_call_zalo);
        }
    }
}