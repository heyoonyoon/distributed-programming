package contract;

import common.enums.EApplicationStatus;
import common.vo.ApplicationDetail;
import common.vo.MedicalHistory;
import common.vo.VehicleInfo;

import java.util.Date;

public class InsuranceApplication {
    private String applicationId;
    private Date appliedAt;
    private EApplicationStatus status;
    private VehicleInfo vehicleInfo;
    private MedicalHistory medicalHistory;
    private String productId;
    private String holderName;

    public InsuranceApplication(String applicationId, String productId, String holderName,
                                VehicleInfo vehicleInfo, MedicalHistory medicalHistory) {
        this.applicationId = applicationId;
        this.productId = productId;
        this.holderName = holderName;
        this.vehicleInfo = vehicleInfo;
        this.medicalHistory = medicalHistory;
        this.appliedAt = new Date();
        this.status = EApplicationStatus.PENDING;
    }

    public String getApplicationId() { return applicationId; }
    public EApplicationStatus getStatus() { return status; }
    public void setStatus(EApplicationStatus status) { this.status = status; }
    public String getProductId() { return productId; }
    public String getHolderName() { return holderName; }
    public Date getAppliedAt() { return appliedAt; }
    public VehicleInfo getVehicleInfo() { return vehicleInfo; }
    public MedicalHistory getMedicalHistory() { return medicalHistory; }

    public ApplicationDetail getDetail() {
        return null;
    }

    public void cancel() {
    }
}
