package ntu.edu.seniorcare.contact.contacts;

import java.io.Serializable;

// Data model for a single contact
public class ContactInfo implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    // Đã xóa: private Uri photoUri;

    public ContactInfo(String id, String name, String phoneNumber) { // Constructor đã sửa
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        // Đã xóa: this.photoUri = photoUri;
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

    // Đã xóa: public Uri getPhotoUri() { return photoUri; }
    // Đã xóa: public void setPhotoUri(Uri photoUri) { this.photoUri = photoUri; }
}