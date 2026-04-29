package common.vo;

public class MedicalHistory {
    private String conditions;
    private String hospitalRecords;

    public MedicalHistory(String conditions, String hospitalRecords) {
        this.conditions = conditions;
        this.hospitalRecords = hospitalRecords;
    }

    public String getConditions() { return conditions; }
    public String getHospitalRecords() { return hospitalRecords; }
}
