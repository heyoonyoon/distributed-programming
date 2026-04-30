package user;

import common.enums.EReviewResult;
import review.BenefitPaymentReview;
import review.EnrollmentReview;

public class InsuranceEmployee extends User {
    private String employeeId;
    private String department;
    private int currentLoad;

    public InsuranceEmployee(String userId, String name, String department, String email, String phone) {
        this.userId = userId;
        this.name = name;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.currentLoad = 0;
    }

    public String getEmployeeId() { return userId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public int getCurrentLoad() { return currentLoad; }
    public void incrementLoad() { currentLoad++; }

    public EnrollmentReview reviewEnrollment(String applicationId, EReviewResult result) {
        return null;
    }

    public BenefitPaymentReview reviewBenefitPayment(String claimId, EReviewResult result) {
        return null;
    }

    public void requestDeptConsultation(String reviewId, String reason) {
    }
}
