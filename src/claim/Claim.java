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
    protected int paidAmount;
    protected String claimReason;
    protected String documents;

    public Claim(String claimId, Date claimDate, int requestAmount, EClaimStatus status,
                 String claimReason, String documents, int paidAmount) {
        this.claimId = claimId;
        this.claimDate = claimDate;
        this.requestAmount = requestAmount;
        this.status = status;
        this.assignedStaff = "홍길동 심사관";
        this.staffContact = "02-1234-5678";
        this.claimReason = claimReason;
        this.documents = documents;
        this.paidAmount = paidAmount;
    }

    public String getClaimId() { return claimId; }
    public Date getClaimDate() { return claimDate; }
    public int getRequestAmount() { return requestAmount; }
    public int getPaidAmount() { return paidAmount; }
    public String getClaimReason() { return claimReason; }
    public String getDocuments() { return documents; }
    public String getAssignedStaff() { return assignedStaff; }
    public String getStaffContact() { return staffContact; }

    public abstract String getClaimType();

    public void submit() {
    }

    public EClaimStatus getStatus() {
        return status;
    }
}
