package claim;

import common.enums.EClaimComplexity;
import common.enums.EClaimStatus;

import java.io.File;
import java.util.Date;

public class HealthInsuranceClaim extends Claim {
    private String hospitalName;
    private String diagnosisCode;
    private Date treatmentDate;
    private int receiptAmount;
    private EClaimComplexity complexity;

    public HealthInsuranceClaim(String claimId, Date claimDate, int requestAmount,
                                EClaimStatus status, String hospitalName, String diagnosisCode) {
        super(claimId, claimDate, requestAmount, status);
        this.hospitalName = hospitalName;
        this.diagnosisCode = diagnosisCode;
        this.treatmentDate = claimDate;
        this.complexity = EClaimComplexity.SIMPLE;
    }

    @Override
    public String getClaimType() { return "의료보험 청구"; }

    public void attachDocument(File file) {
    }

    public boolean isSimpleClaim() {
        return complexity == EClaimComplexity.SIMPLE;
    }
}
