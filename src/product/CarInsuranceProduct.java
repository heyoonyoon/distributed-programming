package product;

import user.Policyholder;

public class CarInsuranceProduct extends InsuranceProduct {
    private String vehicleType;
    private String driverScopeType;

    @Override
    public int calculatePremium(Policyholder holder) {
        return 0;
    }
}
