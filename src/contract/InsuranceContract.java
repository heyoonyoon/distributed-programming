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

    public ContractDetail getContractDetail() {
        return null;
    }

    public void suspend() {
    }

    public void terminate() {
    }

    public File generatePdf() {
        return null;
    }
}
