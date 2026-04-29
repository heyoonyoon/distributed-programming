package common.vo;

import java.util.Date;

public class Receipt {
    private String paymentId;
    private int amount;
    private Date paidAt;

    public Receipt(String paymentId, int amount, Date paidAt) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public String getPaymentId() { return paymentId; }
    public int getAmount() { return amount; }
    public Date getPaidAt() { return paidAt; }
}
