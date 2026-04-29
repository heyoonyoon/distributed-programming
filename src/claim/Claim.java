package claim;

import common.enums.EClaimStatus;

import java.util.Date;

public abstract class Claim {
    protected String claimId;
    protected Date claimDate;
    protected int requestAmount;
    protected EClaimStatus status;

    public void submit() {
    }

    public EClaimStatus getStatus() {
        return status;
    }
}
