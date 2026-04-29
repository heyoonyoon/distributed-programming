package common.vo;

public class AccidentSummary {
    private int accidentCount;
    private int totalPaidAmount;
    private String licenseStatus;

    public AccidentSummary(int accidentCount, int totalPaidAmount, String licenseStatus) {
        this.accidentCount = accidentCount;
        this.totalPaidAmount = totalPaidAmount;
        this.licenseStatus = licenseStatus;
    }

    public int getAccidentCount() { return accidentCount; }
    public int getTotalPaidAmount() { return totalPaidAmount; }
    public String getLicenseStatus() { return licenseStatus; }
}
