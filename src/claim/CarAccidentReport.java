package claim;

import java.io.File;
import java.util.Date;

public class CarAccidentReport extends Claim {
    private Date accidentDate;
    private String accidentLocation;
    private String accidentType;
    private String vehicleNumber;
    private boolean hasInjury;
    private int injuredCount;

    public void attachPhoto(File file) {
    }
}
