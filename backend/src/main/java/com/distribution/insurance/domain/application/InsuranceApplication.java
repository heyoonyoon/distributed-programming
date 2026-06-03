package com.distribution.insurance.domain.application;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.service.IllegalStateTransitionException;
import com.distribution.insurance.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_application")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InsuranceApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime appliedAt;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private Policyholder applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InsuranceProduct product;

    @Embedded
    private VehicleInfo vehicleInfo;      // 자동차상품만, nullable

    @Embedded
    private MedicalHistory medicalHistory; // 의료상품만, nullable

    public InsuranceApplication(Policyholder applicant, InsuranceProduct product,
                                VehicleInfo vehicleInfo, MedicalHistory medicalHistory) {
        validateTypeConsistency(product, vehicleInfo, medicalHistory);
        this.applicant = applicant;
        this.product = product;
        this.vehicleInfo = vehicleInfo;
        this.medicalHistory = medicalHistory;
        this.status = ApplicationStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    private static void validateTypeConsistency(InsuranceProduct product,
                                                VehicleInfo vehicleInfo, MedicalHistory medicalHistory) {
        if (product instanceof CarInsuranceProduct) {
            if (vehicleInfo == null || medicalHistory != null) {
                throw new InvalidRequestException("자동차보험은 차량정보가 필수이며 의료고지는 입력할 수 없습니다.");
            }
        } else if (product instanceof HealthInsuranceProduct) {
            if (medicalHistory == null || vehicleInfo != null) {
                throw new InvalidRequestException("의료보험은 의료고지가 필수이며 차량정보는 입력할 수 없습니다.");
            }
        } else {
            throw new InvalidRequestException("지원하지 않는 상품 종류입니다.");
        }
    }

    /** PENDING 건만 취소 가능(UC02). 그 외 상태는 상태 전이 위반 → 409. */
    public void cancel() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateTransitionException("심사 대기 상태의 신청만 취소할 수 있습니다.");
        }
        this.status = ApplicationStatus.CANCELLED;
    }
}
