package common.vo;

public class ProductInfo {
    private String productId;
    private String productName;
    private int basePremium;

    public ProductInfo(String productId, String productName, int basePremium) {
        this.productId = productId;
        this.productName = productName;
        this.basePremium = basePremium;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getBasePremium() { return basePremium; }
}
