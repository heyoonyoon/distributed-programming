package product;

import common.vo.ProductInfo;
import user.Policyholder;

public abstract class InsuranceProduct {
    protected String productId;
    protected String productName;
    protected String description;
    protected int basePremium;

    public abstract int calculatePremium(Policyholder holder);

    public ProductInfo getProductInfo() {
        return null;
    }
}
