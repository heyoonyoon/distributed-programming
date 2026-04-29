package external;

import common.vo.AccidentSummary;
import java.util.Date;

public class AccidentHistory {
    private String ssn;
    private int accidentCount;
    private int totalPaidAmount;
    private String licenseStatus;
    private Date fetchedAt;

    public static AccidentHistory fetch(String ssn) {
        return null;
    }

    public AccidentSummary getSummary() {
        return null;
    }
}
