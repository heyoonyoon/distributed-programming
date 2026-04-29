package user;

import claim.CarAccidentReport;
import claim.HealthInsuranceClaim;
import common.enums.EPaymentMethod;
import common.vo.AccidentInfo;
import common.vo.HealthClaimInfo;
import common.vo.ProfitAnalysisResult;
import contract.InsuranceApplication;
import contract.Payment;

import java.util.Date;

public class Policyholder extends User {
    private String ssn;
    private Date birthDate;
    private String address;
    private String bankAccount;

    public InsuranceApplication applyInsurance(String productId) {
        return null;
    }

    public HealthInsuranceClaim submitHealthClaim(String contractId, HealthClaimInfo claimInfo) {
        return null;
    }

    public CarAccidentReport reportCarAccident(String contractId, AccidentInfo accidentInfo) {
        return null;
    }

    public Payment payPremium(String contractId, EPaymentMethod method) {
        return null;
    }

    public ProfitAnalysisResult getProfitAnalysis(String contractId) {
        return null;
    }
}
