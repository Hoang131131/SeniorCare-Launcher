package ntu.edu.seniorcare.contact.missedcalls;

public class MissedCallHeaderItem extends MissedCallGroupItem {
    private String dateHeader;

    public MissedCallHeaderItem(String dateHeader) {
        this.dateHeader = dateHeader;
    }

    public String getDateHeader() {
        return dateHeader;
    }

    @Override
    public int getType() {
        return TYPE_HEADER;
    }
}