package common.vo;

public class VehicleInfo {
    private String vehicleNumber;
    private String vehicleType;

    public VehicleInfo(String vehicleNumber, String vehicleType) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
    }

    public String getVehicleNumber() { return vehicleNumber; }
    public String getVehicleType() { return vehicleType; }
}
