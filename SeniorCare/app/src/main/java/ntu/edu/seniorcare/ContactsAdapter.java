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
import android.util.Log; // Thêm Log để gỡ lỗi

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private static final String ZALO_PACKAGE_NAME = "com.zing.zalo";
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
                if (inputStream != null) {
                    Drawable photo = Drawable.createFromStream(inputStream, contact.getPhotoUri().toString());
                    holder.contactPhoto.setImageDrawable(photo);
                    inputStream.close(); // Đóng InputStream
                } else {
                    holder.contactPhoto.setImageResource(R.drawable.ic_contacts);
                }
            } catch (Exception e) {
                // Fallback to default icon if photo cannot be loaded
                holder.contactPhoto.setImageResource(R.drawable.ic_contacts);
                Log.e("ContactsAdapter", "Error loading contact photo: " + e.getMessage());
            }
        } else {
            holder.contactPhoto.setImageResource(R.drawable.ic_contacts);
        }

        // Button: Gọi thường
        holder.btnCallPhone.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                String phoneNumber = contact.getPhoneNumber();
                // Chuẩn hóa số điện thoại cho mục đích gọi (loại bỏ khoảng trắng, dấu gạch ngang)
                phoneNumber = phoneNumber.replaceAll("[^\\d+]", "");

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + phoneNumber));

                try {
                    context.startActivity(callIntent);
                } catch (SecurityException e) {
                    // Xử lý trường hợp SecurityException nếu quyền bị thu hồi đột ngột
                    Toast.makeText(context, "Không có quyền thực hiện cuộc gọi. Vui lòng kiểm tra cài đặt ứng dụng.", Toast.LENGTH_LONG).show();
                    Log.e("ContactsAdapter", "SecurityException when trying to call: " + e.getMessage());
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(context, "Không có ứng dụng gọi điện nào được tìm thấy.", Toast.LENGTH_SHORT).show();
                    Log.e("ContactsAdapter", "No dialer app found for Intent.ACTION_CALL: " + e.getMessage());
                }
            } else {
                Toast.makeText(context, "Cần cấp quyền CALL_PHONE để thực hiện cuộc gọi. Vui lòng cấp quyền trong cài đặt ứng dụng.", Toast.LENGTH_LONG).show();
                // Gợi ý: Trong một ứng dụng thực tế, bạn sẽ cần một cơ chế để yêu cầu quyền này từ Activity
                // Ví dụ: sử dụng ActivityResultLauncher hoặc gửi Broadcast/EventBus tới Activity cha.
            }
        });

        // Button: Gọi Zalo
        holder.btnCallZalo.setOnClickListener(v -> {
            String phoneNumber = contact.getPhoneNumber();
            // Xóa mọi ký tự không phải số khỏi số điện thoại để chuẩn hóa cho Zalo deep link
            phoneNumber = phoneNumber.replaceAll("[^0-9]", "");

            if (isPackageInstalled(ZALO_PACKAGE_NAME, context.getPackageManager())) {
                // Ưu tiên thử mở chat với số điện thoại qua deep link
                // Lưu ý: Các deep link của Zalo không có tài liệu chính thức và có thể thay đổi
                // Format zalo://chat?phone=84... (số điện thoại không có số 0 đầu, có +84 hoặc 84)
                // Hoặc zalo://chat?id= (Zalo ID)
                // Tôi sẽ thử với số đã chuẩn hóa. Nếu không được, bạn có thể cần thử thêm các định dạng số khác (vd: thêm "84" phía trước nếu số bắt đầu bằng 0)
                String zaloUriString = "zalo://chat?phone=" + phoneNumber;
                if (phoneNumber.startsWith("0")) { // Zalo deep link thường không cần số 0 ở đầu
                    zaloUriString = "zalo://chat?phone=84" + phoneNumber.substring(1);
                }

                Intent zaloIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(zaloUriString));
                zaloIntent.setPackage(ZALO_PACKAGE_NAME); // Chỉ định rõ package Zalo

                try {
                    context.startActivity(zaloIntent);
                    Log.d("ContactsAdapter", "Successfully launched Zalo chat with: " + phoneNumber);
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e("ContactsAdapter", "Zalo deep link failed for phone number: " + phoneNumber + " using URI: " + zaloUriString + ". Trying to open Zalo general app.", e);
                    // Nếu deep link không hoạt động, mở ứng dụng Zalo chung
                    Intent launchZalo = context.getPackageManager().getLaunchIntentForPackage(ZALO_PACKAGE_NAME);
                    if (launchZalo != null) {
                        launchZalo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            context.startActivity(launchZalo);
                            Toast.makeText(context, "Không thể mở chat Zalo trực tiếp. Đã mở ứng dụng Zalo.", Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                            Toast.makeText(context, "Không thể mở ứng dụng Zalo.", Toast.LENGTH_SHORT).show();
                            Log.e("ContactsAdapter", "Failed to launch Zalo general app: " + ex.getMessage());
                        }
                    } else {
                        Toast.makeText(context, "Không thể mở ứng dụng Zalo. Vui lòng thử lại hoặc cài đặt Zalo.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(context, "Ứng dụng Zalo chưa được cài đặt.", Toast.LENGTH_LONG).show();
                // Có thể mở Google Play Store để cài đặt Zalo
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ZALO_PACKAGE_NAME)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + ZALO_PACKAGE_NAME)));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    /**
     * Kiểm tra xem một package ứng dụng có được cài đặt trên thiết bị hay không.
     * @param packageName Tên package của ứng dụng (ví dụ: "com.zing.zalo").
     * @param packageManager PackageManager của Context.
     * @return true nếu package đã được cài đặt, ngược lại false.
     */
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
        Button btnCallZalo; // Đảm bảo ID này khớp với layout XML

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
            btnCallPhone = itemView.findViewById(R.id.btn_call_phone);
            btnCallZalo = itemView.findViewById(R.id.btn_call_zalo); // Đảm bảo ID này khớp với layout XML
        }
    }
}