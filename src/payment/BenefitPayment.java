package payment;

import common.enums.EPaymentStatus;

import java.util.Date;

public class BenefitPayment {
    private String paymentId;
    private int paidAmount;
    private Date paidAt;
    private String bankAccount;
    private EPaymentStatus status;

    public BenefitPayment(String paymentId, int paidAmount, String bankAccount) {
        this.paymentId = paymentId;
        this.paidAmount = paidAmount;
        this.paidAt = new Date();
        this.bankAccount = bankAccount;
        this.status = EPaymentStatus.SUCCESS;
    }

    public String getPaymentId() { return paymentId; }
    public int getPaidAmount() { return paidAmount; }
    public Date getPaidAt() { return paidAt; }
    public EPaymentStatus getStatus() { return status; }

    public boolean transfer() {
        return true;
    }

    public void notifyPolicyholder(String email, String phone) {
    }
}
