package ntu.edu.seniorcare;

import android.Manifest;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

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

        // Button: Gọi thường
        holder.btnCallPhone.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                String phoneNumber = contact.getPhoneNumber();
                phoneNumber = phoneNumber.replaceAll("[^\\d+]", "");

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + phoneNumber));

                try {
                    context.startActivity(callIntent);
                } catch (SecurityException e) {
                    Toast.makeText(context, "Không có quyền thực hiện cuộc gọi. Vui lòng kiểm tra cài đặt ứng dụng.", Toast.LENGTH_LONG).show();
                    Log.e("ContactsAdapter", "SecurityException when trying to call: " + e.getMessage());
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(context, "Không có ứng dụng gọi điện nào được tìm thấy.", Toast.LENGTH_SHORT).show();
                    Log.e("ContactsAdapter", "No dialer app found for Intent.ACTION_CALL: " + e.getMessage());
                }
            } else {
                Toast.makeText(context, "Cần cấp quyền CALL_PHONE để thực hiện cuộc gọi. Vui lòng cấp quyền trong cài đặt ứng dụng.", Toast.LENGTH_LONG).show();
            }
        });

        // Button: Gọi Zalo
        holder.btnCallZalo.setOnClickListener(v -> {
            String phoneNumber = contact.getPhoneNumber();
            phoneNumber = phoneNumber.replaceAll("[^0-9]", "");

            if (isPackageInstalled(ZALO_PACKAGE_NAME, context.getPackageManager())) {
                String zaloUriString = "zalo://chat?phone=" + phoneNumber;
                if (phoneNumber.startsWith("0")) {
                    zaloUriString = "zalo://chat?phone=84" + phoneNumber.substring(1);
                }

                Intent zaloIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(zaloUriString));
                zaloIntent.setPackage(ZALO_PACKAGE_NAME);

                try {
                    context.startActivity(zaloIntent);
                    Log.d("ContactsAdapter", "Successfully launched Zalo chat with: " + phoneNumber);
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e("ContactsAdapter", "Zalo deep link failed for phone number: " + phoneNumber + " using URI: " + zaloUriString + ". Trying to open Zalo general app.", e);
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

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView contactName;
        TextView contactPhone;
        Button btnCallPhone;
        Button btnCallZalo;
        // Đã xóa: ImageView contactPhoto; // Không còn ImageView nữa

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
            btnCallPhone = itemView.findViewById(R.id.btn_call_phone);
            btnCallZalo = itemView.findViewById(R.id.btn_call_zalo);
        }
    }
}