package common.vo;

import common.enums.EContractStatus;
import java.util.Date;

public class ContractDetail {
    private String contractId;
    private Date startDate;
    private Date endDate;
    private EContractStatus status;
    private int monthlyPremium;

    public ContractDetail(String contractId, Date startDate, Date endDate, EContractStatus status, int monthlyPremium) {
        this.contractId = contractId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.monthlyPremium = monthlyPremium;
    }

    public String getContractId() { return contractId; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public EContractStatus getStatus() { return status; }
    public int getMonthlyPremium() { return monthlyPremium; }
}
