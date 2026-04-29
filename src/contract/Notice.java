package contract;

import java.util.Date;

public class Notice {
    private String noticeId;
    private Date issuedAt;
    private int dueAmount;
    private int overdueDays;
    private boolean isTerminationWarning;

    public boolean send(String email, String phone) {
        return false;
    }
}
