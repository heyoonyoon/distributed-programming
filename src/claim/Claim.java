package claim;

import common.enums.EClaimStatus;

import java.util.Date;

public abstract class Claim {
    protected String claimId;
    protected Date claimDate;
    protected int requestAmount;
    protected EClaimStatus status;
    protected String assignedStaff;
    protected String staffContact;

    public Claim(String claimId, Date claimDate, int requestAmount, EClaimStatus status) {
        this.claimId = claimId;
        this.claimDate = claimDate;
        this.requestAmount = requestAmount;
        this.status = status;
        this.assignedStaff = "홍길동 심사관";
        this.staffContact = "02-1234-5678";
    }

    public String getClaimId() { return claimId; }
    public Date getClaimDate() { return claimDate; }
    public int getRequestAmount() { return requestAmount; }
    public String getAssignedStaff() { return assignedStaff; }
    public String getStaffContact() { return staffContact; }

    public abstract String getClaimType();

    public void submit() {
    }

    public EClaimStatus getStatus() {
        return status;
    }
}
