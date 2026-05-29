package ntu.edu.seniorcare.contact.missedcalls;

public abstract class MissedCallGroupItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_CALL_ITEM = 1;

    public abstract int getType();
}