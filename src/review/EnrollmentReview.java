package review;

import external.AccidentHistory;

public class EnrollmentReview extends Review {
    private double surchargeRate;
    private int adjustedPremium;
    private AccidentHistory accidentHistory;

    public int applySurcharge(double rate) {
        return 0;
    }

    public AccidentHistory fetchAccidentHistory(String ssn) {
        return null;
    }
}
