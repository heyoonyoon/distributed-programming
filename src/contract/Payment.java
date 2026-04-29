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

    public boolean process() {
        return false;
    }

    public Receipt getReceipt() {
        return null;
    }
}
