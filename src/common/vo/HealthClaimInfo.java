package common.vo;

import java.util.Date;

public class HealthClaimInfo {
    private String hospitalName;
    private String diagnosisCode;
    private Date treatmentDate;
    private int receiptAmount;

    public HealthClaimInfo(String hospitalName, String diagnosisCode, Date treatmentDate, int receiptAmount) {
        this.hospitalName = hospitalName;
        this.diagnosisCode = diagnosisCode;
        this.treatmentDate = treatmentDate;
        this.receiptAmount = receiptAmount;
    }

    public String getHospitalName() { return hospitalName; }
    public String getDiagnosisCode() { return diagnosisCode; }
    public Date getTreatmentDate() { return treatmentDate; }
    public int getReceiptAmount() { return receiptAmount; }
}
