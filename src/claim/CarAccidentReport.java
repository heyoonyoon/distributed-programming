package claim;

import common.enums.EClaimStatus;

import java.io.File;
import java.util.Date;

public class CarAccidentReport extends Claim {
    private Date accidentDate;
    private String accidentLocation;
    private String accidentType;
    private String vehicleNumber;
    private boolean hasInjury;
    private int injuredCount;

    public CarAccidentReport(String claimId, Date claimDate, int requestAmount,
                             EClaimStatus status, String accidentLocation, String accidentType, String vehicleNumber) {
        super(claimId, claimDate, requestAmount, status);
        this.accidentDate = claimDate;
        this.accidentLocation = accidentLocation;
        this.accidentType = accidentType;
        this.vehicleNumber = vehicleNumber;
        this.hasInjury = false;
        this.injuredCount = 0;
    }

    @Override
    public String getClaimType() { return "자동차 사고 접수"; }

    public void attachPhoto(File file) {
    }
}
