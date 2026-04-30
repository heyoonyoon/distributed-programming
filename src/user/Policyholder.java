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

    public Policyholder(String userId, String name, String email, String phone, String ssn, String address, String bankAccount) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.ssn = ssn;
        this.address = address;
        this.bankAccount = bankAccount;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getBankAccount() { return bankAccount; }
    public String getSsn() { return ssn; }
    public String getBirthDate() { return ssn.length() >= 6 ? ssn.substring(0, 6) : ssn; }

    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

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
