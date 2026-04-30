package product;

import user.Policyholder;

public class HealthInsuranceProduct extends InsuranceProduct {
    private int maxHospitalizationDays;

    public HealthInsuranceProduct(String productId, String productName, String description, int basePremium, int maxHospitalizationDays) {
        super(productId, productName, description, basePremium);
        this.maxHospitalizationDays = maxHospitalizationDays;
    }

    public int getMaxHospitalizationDays() { return maxHospitalizationDays; }

    @Override
    public int calculatePremium(Policyholder holder) {
        return basePremium;
    }

    @Override
    public String getPremiumBasis() {
        return "나이·병력 기반 산출, 최대 입원 보장 " + maxHospitalizationDays + "일";
    }
}
