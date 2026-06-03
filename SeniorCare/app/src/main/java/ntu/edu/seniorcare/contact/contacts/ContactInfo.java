package ntu.edu.seniorcare.contact.contacts;

import java.io.Serializable;

public class ContactInfo implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;

    public ContactInfo(String id, String name, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
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

}