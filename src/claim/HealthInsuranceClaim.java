package claim;

import common.enums.EClaimComplexity;

import java.io.File;
import java.util.Date;

public class HealthInsuranceClaim extends Claim {
    private String hospitalName;
    private String diagnosisCode;
    private Date treatmentDate;
    private int receiptAmount;
    private EClaimComplexity complexity;

    public void attachDocument(File file) {
    }

    public boolean isSimpleClaim() {
        return false;
    }
}
