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

    public ApplicationDetail getDetail() {
        return null;
    }

    public void cancel() {
    }
}
