package product;

import common.vo.ProductInfo;
import user.Policyholder;

public abstract class InsuranceProduct {
    protected String productId;
    protected String productName;
    protected String description;
    protected int basePremium;
    protected java.util.List<CoverageItem> coverageItems = new java.util.ArrayList<>();

    public InsuranceProduct(String productId, String productName, String description, int basePremium) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.basePremium = basePremium;
    }

    public void addCoverageItem(CoverageItem item) {
        coverageItems.add(item);
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getDescription() { return description; }
    public int getBasePremium() { return basePremium; }
    public java.util.List<CoverageItem> getCoverageItems() { return coverageItems; }

    public abstract int calculatePremium(Policyholder holder);
    public abstract String getPremiumBasis();

    public ProductInfo getProductInfo() {
        return null;
    }
}
