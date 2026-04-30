package product;

public class CoverageItem {
    private String itemId;
    private String itemName;
    private int coverageLimit;
    private int deductible;

    public CoverageItem(String itemId, String itemName, int coverageLimit, int deductible) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.coverageLimit = coverageLimit;
        this.deductible = deductible;
    }

    public String getItemName() { return itemName; }
    public int getCoverageLimit() { return coverageLimit; }
    public int getDeductible() { return deductible; }
}
