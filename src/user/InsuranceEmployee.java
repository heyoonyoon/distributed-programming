package user;

import common.enums.EReviewResult;
import review.BenefitPaymentReview;
import review.EnrollmentReview;

public class InsuranceEmployee extends User {
    private String employeeId;
    private String department;
    private int currentLoad;

    public EnrollmentReview reviewEnrollment(String applicationId, EReviewResult result) {
        return null;
    }

    public BenefitPaymentReview reviewBenefitPayment(String claimId, EReviewResult result) {
        return null;
    }

    public void requestDeptConsultation(String reviewId, String reason) {
    }
}
