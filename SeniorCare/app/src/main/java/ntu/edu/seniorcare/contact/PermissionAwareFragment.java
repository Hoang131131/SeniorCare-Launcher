package ntu.edu.seniorcare.contact;

public interface PermissionAwareFragment {
    void onPermissionsGranted();
    void onPermissionsDenied();
}
