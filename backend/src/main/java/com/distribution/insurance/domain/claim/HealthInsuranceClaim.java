package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 의료보험 청구(UC05). 청구금액으로 complexity 판별. */
@Entity
@Table(name = "health_insurance_claim")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class HealthInsuranceClaim extends Claim {

    private String hospitalName;
    private String diagnosisCode;
    private LocalDate treatmentDate;
    private int receiptAmount;

    @Enumerated(EnumType.STRING)
    private ClaimComplexity complexity;

    @ElementCollection
    @CollectionTable(name = "health_claim_attachment", joinColumns = @JoinColumn(name = "claim_id"))
    private List<ClaimAttachment> attachments = new ArrayList<>();

    public HealthInsuranceClaim(InsuranceContract contract, int requestAmount,
                                String hospitalName, String diagnosisCode, LocalDate treatmentDate,
                                int receiptAmount, ClaimComplexity complexity) {
        super(contract, requestAmount);
        this.hospitalName = hospitalName;
        this.diagnosisCode = diagnosisCode;
        this.treatmentDate = treatmentDate;
        this.receiptAmount = receiptAmount;
        this.complexity = complexity;
    }

    public void addAttachment(ClaimAttachment attachment) {
        this.attachments.add(attachment);
    }
}
