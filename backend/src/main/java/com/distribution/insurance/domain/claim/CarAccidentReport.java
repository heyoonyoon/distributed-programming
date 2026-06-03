package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 자동차사고 접수(UC09). 접수 단계라 청구금액 미정(requestAmount=0). 심사/지급 흐름 없음. */
@Entity
@Table(name = "car_accident_report")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CarAccidentReport extends Claim {

    private LocalDate accidentDate;
    private String accidentLocation;
    private String accidentType;
    private String vehicleNumber;
    private boolean hasInjury;
    private int injuredCount;

    @ElementCollection
    @CollectionTable(name = "car_accident_attachment", joinColumns = @JoinColumn(name = "claim_id"))
    private List<ClaimAttachment> attachments = new ArrayList<>();

    public CarAccidentReport(InsuranceContract contract, LocalDate accidentDate, String accidentLocation,
                             String accidentType, String vehicleNumber, boolean hasInjury, int injuredCount) {
        super(contract, 0);
        this.accidentDate = accidentDate;
        this.accidentLocation = accidentLocation;
        this.accidentType = accidentType;
        this.vehicleNumber = vehicleNumber;
        this.hasInjury = hasInjury;
        this.injuredCount = injuredCount;
    }

    public void addAttachment(ClaimAttachment attachment) {
        this.attachments.add(attachment);
    }
}
