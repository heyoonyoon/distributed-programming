package review;

import common.enums.EReviewResult;
import payment.BenefitPayment;
import user.InsuranceEmployee;

import java.util.Date;

public class BenefitPaymentReview extends Review {
    private String assignedStaffId;
    private String assignedStaffName;
    private String claimId;
    private boolean locked;

    public BenefitPaymentReview(String reviewId, String claimId, String assignedStaffId, String assignedStaffName) {
        this.reviewId = reviewId;
        this.claimId = claimId;
        this.assignedStaffId = assignedStaffId;
        this.assignedStaffName = assignedStaffName;
        this.locked = false;
    }

    public String getClaimId() { return claimId; }
    public String getAssignedStaffId() { return assignedStaffId; }
    public String getAssignedStaffName() { return assignedStaffName; }
    public boolean isLocked() { return locked; }
    public void lock() { this.locked = true; }
    public void unlock() { this.locked = false; }

    public void assignStaff(InsuranceEmployee employee) {
    }

    public BenefitPayment approve(int paidAmount, String bankAccount) {
        this.result = EReviewResult.APPROVED;
        this.reviewedAt = new Date();
        String paymentId = "BPY-" + System.currentTimeMillis();
        BenefitPayment bp = new BenefitPayment(paymentId, paidAmount, bankAccount);
        bp.transfer();
        return bp;
    }

    public void reject(String reason) {
        this.result = EReviewResult.REJECTED;
        this.comment = reason;
        this.reviewedAt = new Date();
    }

    @Override
    public void confirm(EReviewResult result, String comment) {
        this.result = result;
        this.comment = comment;
        this.reviewedAt = new Date();
    }

    @Override
    public EReviewResult getResult() { return result; }
}
