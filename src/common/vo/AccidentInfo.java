package common.vo;

import java.util.Date;

public class AccidentInfo {
    private Date accidentDate;
    private String accidentLocation;
    private String accidentType;
    private String vehicleNumber;
    private boolean hasInjury;
    private int injuredCount;

    public AccidentInfo(Date accidentDate, String accidentLocation, String accidentType,
                        String vehicleNumber, boolean hasInjury, int injuredCount) {
        this.accidentDate = accidentDate;
        this.accidentLocation = accidentLocation;
        this.accidentType = accidentType;
        this.vehicleNumber = vehicleNumber;
        this.hasInjury = hasInjury;
        this.injuredCount = injuredCount;
    }

    public Date getAccidentDate() { return accidentDate; }
    public String getAccidentLocation() { return accidentLocation; }
    public String getAccidentType() { return accidentType; }
    public String getVehicleNumber() { return vehicleNumber; }
    public boolean isHasInjury() { return hasInjury; }
    public int getInjuredCount() { return injuredCount; }
}
