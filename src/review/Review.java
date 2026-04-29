package review;

import common.enums.EReviewResult;

import java.util.Date;

public abstract class Review {
    protected String reviewId;
    protected Date reviewedAt;
    protected EReviewResult result;
    protected String comment;

    public void confirm(EReviewResult result, String comment) {
    }

    public EReviewResult getResult() {
        return result;
    }
}
