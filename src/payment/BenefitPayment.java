package payment;

import common.enums.EPaymentStatus;

import java.util.Date;

public class BenefitPayment {
    private String paymentId;
    private int paidAmount;
    private Date paidAt;
    private String bankAccount;
    private EPaymentStatus status;

    public boolean transfer() {
        return false;
    }

    public void notifyPolicyholder(String email, String phone) {
    }
}
