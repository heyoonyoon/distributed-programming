package common.vo;

import common.enums.EApplicationStatus;
import java.util.Date;

public class ApplicationDetail {
    private String applicationId;
    private EApplicationStatus status;
    private Date appliedAt;

    public ApplicationDetail(String applicationId, EApplicationStatus status, Date appliedAt) {
        this.applicationId = applicationId;
        this.status = status;
        this.appliedAt = appliedAt;
    }

    public String getApplicationId() { return applicationId; }
    public EApplicationStatus getStatus() { return status; }
    public Date getAppliedAt() { return appliedAt; }
}
