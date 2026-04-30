package contract;

import java.util.Date;

public class Notice {
    private String noticeId;
    private Date issuedAt;
    private int dueAmount;
    private int overdueDays;
    private boolean isTerminationWarning;
    private String contractName;
    private Date dueDate;

    public Notice(String noticeId, String contractName, Date dueDate, int dueAmount, int overdueDays) {
        this.noticeId = noticeId;
        this.contractName = contractName;
        this.dueDate = dueDate;
        this.dueAmount = dueAmount;
        this.overdueDays = overdueDays;
        this.issuedAt = new Date();
        this.isTerminationWarning = overdueDays > 30;
    }

    public String getNoticeId() { return noticeId; }
    public String getContractName() { return contractName; }
    public Date getDueDate() { return dueDate; }
    public int getDueAmount() { return dueAmount; }
    public int getOverdueDays() { return overdueDays; }
    public boolean isTerminationWarning() { return isTerminationWarning; }
    public int getInterest() { return (int)(dueAmount * 0.015 * overdueDays / 30.0); }

    public boolean send(String email, String phone) {
        return false;
    }
}
