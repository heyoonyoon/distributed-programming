package review;

import common.enums.EReviewResult;
import external.AccidentHistory;

import java.util.Date;

public class EnrollmentReview extends Review {
    private double surchargeRate;
    private int adjustedPremium;
    private AccidentHistory accidentHistory;
    private String applicationId;
    private boolean locked;

    public EnrollmentReview(String reviewId, String applicationId) {
        this.reviewId = reviewId;
        this.applicationId = applicationId;
        this.locked = false;
    }

    public String getApplicationId() { return applicationId; }
    public boolean isLocked() { return locked; }
    public void lock() { this.locked = true; }
    public void unlock() { this.locked = false; }
    public double getSurchargeRate() { return surchargeRate; }
    public int getAdjustedPremium() { return adjustedPremium; }
    public AccidentHistory getAccidentHistory() { return accidentHistory; }

    public int applySurcharge(double rate, int basePremium) {
        this.surchargeRate = rate;
        this.adjustedPremium = (int)(basePremium * (1 + rate / 100));
        return adjustedPremium;
    }

    public AccidentHistory fetchAccidentHistory(String ssn) {
        this.accidentHistory = AccidentHistory.fetch(ssn);
        return this.accidentHistory;
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
