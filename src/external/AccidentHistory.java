package external;

import common.vo.AccidentSummary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccidentHistory {
    private String ssn;
    private int accidentCount;
    private int totalPaidAmount;
    private String licenseStatus;
    private Date fetchedAt;
    private List<AccidentRecord> records;

    public AccidentHistory(String ssn, int accidentCount, int totalPaidAmount, String licenseStatus) {
        this.ssn = ssn;
        this.accidentCount = accidentCount;
        this.totalPaidAmount = totalPaidAmount;
        this.licenseStatus = licenseStatus;
        this.fetchedAt = new Date();
        this.records = new ArrayList<>();
    }

    public String getSsn() { return ssn; }
    public int getAccidentCount() { return accidentCount; }
    public int getTotalPaidAmount() { return totalPaidAmount; }
    public String getLicenseStatus() { return licenseStatus; }
    public Date getFetchedAt() { return fetchedAt; }
    public List<AccidentRecord> getRecords() { return records; }

    public static AccidentHistory fetch(String ssn) {
        // 금융감독원 API 시뮬레이션
        AccidentHistory history = new AccidentHistory(ssn, 2, 1500000, "VALID");
        history.records.add(new AccidentRecord(
                new Date(System.currentTimeMillis() - 365L*24*60*60*1000), "쌍방", 800000));
        history.records.add(new AccidentRecord(
                new Date(System.currentTimeMillis() - 2*365L*24*60*60*1000), "단독", 700000));
        return history;
    }

    public AccidentSummary getSummary() {
        return new AccidentSummary(accidentCount, totalPaidAmount, licenseStatus);
    }
}
