package product;

import user.Policyholder;

public class HealthInsuranceProduct extends InsuranceProduct {
    private int maxHospitalizationDays;

    @Override
    public int calculatePremium(Policyholder holder) {
        return 0;
    }
}
