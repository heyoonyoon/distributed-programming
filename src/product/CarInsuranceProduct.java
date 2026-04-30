package product;

import user.Policyholder;

public class CarInsuranceProduct extends InsuranceProduct {
    private String vehicleType;
    private String driverScopeType;

    public CarInsuranceProduct(String productId, String productName, String description, int basePremium, String vehicleType, String driverScopeType) {
        super(productId, productName, description, basePremium);
        this.vehicleType = vehicleType;
        this.driverScopeType = driverScopeType;
    }

    public String getVehicleType() { return vehicleType; }
    public String getDriverScopeType() { return driverScopeType; }

    @Override
    public int calculatePremium(Policyholder holder) {
        return basePremium;
    }

    @Override
    public String getPremiumBasis() {
        return "차량 정보·사고이력 기반 산출, 차종: " + vehicleType + ", 운전자 범위: " + driverScopeType;
    }
}
