package external;

import java.util.Date;

public class AccidentRecord {
    private Date accidentDate;
    private String accidentType;
    private int paidAmount;

    public AccidentRecord(Date accidentDate, String accidentType, int paidAmount) {
        this.accidentDate = accidentDate;
        this.accidentType = accidentType;
        this.paidAmount = paidAmount;
    }

    public Date getAccidentDate() { return accidentDate; }
    public String getAccidentType() { return accidentType; }
    public int getPaidAmount() { return paidAmount; }
}
