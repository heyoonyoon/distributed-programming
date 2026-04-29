package common.vo;

public class ProfitAnalysisResult {
    private int totalPaid;
    private int totalReceived;
    private int netBenefit;

    public ProfitAnalysisResult(int totalPaid, int totalReceived, int netBenefit) {
        this.totalPaid = totalPaid;
        this.totalReceived = totalReceived;
        this.netBenefit = netBenefit;
    }

    public int getTotalPaid() { return totalPaid; }
    public int getTotalReceived() { return totalReceived; }
    public int getNetBenefit() { return netBenefit; }
}
