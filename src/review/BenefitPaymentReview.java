package review;

import payment.BenefitPayment;
import user.InsuranceEmployee;

public class BenefitPaymentReview extends Review {
    private String assignedStaffId;

    public void assignStaff(InsuranceEmployee employee) {
    }

    public BenefitPayment approve() {
        return null;
    }

    public void reject(String reason) {
    }
}
