package contract;

import common.enums.EContractStatus;
import common.vo.ContractDetail;

import java.io.File;
import java.util.Date;

public class InsuranceContract {
    private String contractId;
    private Date startDate;
    private Date endDate;
    private EContractStatus status;
    private int monthlyPremium;
    private String productName;
    private String productType;
    private String beneficiary;
    private String paymentMethod;
    private String specialTerms;

    public InsuranceContract(String contractId, String productName, String productType,
                             Date startDate, Date endDate, int monthlyPremium,
                             String beneficiary, String paymentMethod, String specialTerms) {
        this.contractId = contractId;
        this.productName = productName;
        this.productType = productType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyPremium = monthlyPremium;
        this.status = EContractStatus.ACTIVE;
        this.beneficiary = beneficiary;
        this.paymentMethod = paymentMethod;
        this.specialTerms = specialTerms;
    }

    public String getContractId() { return contractId; }
    public String getProductName() { return productName; }
    public String getProductType() { return productType; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public EContractStatus getStatus() { return status; }
    public int getMonthlyPremium() { return monthlyPremium; }
    public String getBeneficiary() { return beneficiary; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getSpecialTerms() { return specialTerms; }

    public ContractDetail getContractDetail() {
        return null;
    }

    public void suspend() {
        this.status = EContractStatus.SUSPENDED;
    }

    public void terminate() {
        this.status = EContractStatus.TERMINATED;
    }

    public File generatePdf() {
        return null;
    }
}
