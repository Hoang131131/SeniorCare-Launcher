package ntu.edu.seniorcare.contact.missedcalls;

public class MissedCallDetailItem extends MissedCallGroupItem {
    private MissedCallInfo missedCallInfo;

    public MissedCallDetailItem(MissedCallInfo missedCallInfo) {
        this.missedCallInfo = missedCallInfo;
    }

    public MissedCallInfo getMissedCallInfo() {
        return missedCallInfo;
    }

    @Override
    public int getType() {
        return TYPE_CALL_ITEM;
    }
}