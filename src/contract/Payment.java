package contract;

import common.enums.EPaymentMethod;
import common.enums.EPaymentStatus;
import common.vo.Receipt;

import java.util.Date;

public class Payment {
    private String paymentId;
    private int amount;
    private Date paidAt;
    private EPaymentMethod method;
    private EPaymentStatus status;

    public Payment(String paymentId, int amount, EPaymentMethod method) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.method = method;
        this.paidAt = new Date();
        this.status = EPaymentStatus.SUCCESS;
    }

    public String getPaymentId() { return paymentId; }
    public int getAmount() { return amount; }
    public Date getPaidAt() { return paidAt; }
    public EPaymentMethod getMethod() { return method; }
    public EPaymentStatus getStatus() { return status; }

    public boolean process() {
        return true;
    }

    public Receipt getReceipt() {
        return new Receipt(paymentId, amount, paidAt);
    }
}
