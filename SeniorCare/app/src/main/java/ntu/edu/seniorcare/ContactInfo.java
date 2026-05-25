package ntu.edu.seniorcare;

import android.net.Uri;
import java.io.Serializable;

// Data model for a single contact
public class ContactInfo implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private Uri photoUri; // URI của ảnh đại diện

    public ContactInfo(String id, String name, String phoneNumber, Uri photoUri) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.photoUri = photoUri;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }
}