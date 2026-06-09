package com.distribution.insurance.review.service;

import com.distribution.insurance.application.domain.ApplicationStatus;
import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.CarInsuranceProduct;
import com.distribution.insurance.review.domain.AccidentHistory;
import org.hibernate.Hibernate;
import com.distribution.insurance.review.domain.EnrollmentReview;
import com.distribution.insurance.review.domain.ReviewResult;
import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.application.repository.ApplicationRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.review.repository.ReviewRepository;
import com.distribution.insurance.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import com.distribution.insurance.common.service.AccidentHistoryClient;
import com.distribution.insurance.common.service.NotificationSender;

@Service
public class ReviewService {

    private final ApplicationRepository applicationRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AccidentHistoryClient accidentHistoryClient;
    private final NotificationSender notificationSender;
    private final ContractRepository contractRepository;

    public ReviewService(ApplicationRepository applicationRepository,
                         ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         AccidentHistoryClient accidentHistoryClient,
                         NotificationSender notificationSender,
                         ContractRepository contractRepository) {
        this.applicationRepository = applicationRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.accidentHistoryClient = accidentHistoryClient;
        this.notificationSender = notificationSender;
        this.contractRepository = contractRepository;
    }

    /** 심사 상세 + (자동차건) 사고이력 참조 정보. */
    public record ReviewDetail(InsuranceApplication application, AccidentHistory accidentHistory) {}

    @Transactional(readOnly = true)
    public List<InsuranceApplication> pendingApplications() {
        return applicationRepository.findByStatus(ApplicationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public ReviewDetail detail(Long applicationId) {
        InsuranceApplication app = requireApplication(applicationId);
        AccidentHistory history = null;
        if (Hibernate.unproxy(app.getProduct()) instanceof CarInsuranceProduct) {
            history = accidentHistoryClient.fetch(app.getApplicant().getSsn());
        }
        return new ReviewDetail(app, history);
    }

    @Transactional
    public EnrollmentReview confirm(Long reviewerId, Long applicationId,
                                    ReviewResult result, String comment, Double surchargeRate) {
        InsuranceEmployee reviewer = requireEmployee(reviewerId);
        InsuranceApplication app = requireApplication(applicationId);

        EnrollmentReview review = new EnrollmentReview(app, reviewer);
        if (Hibernate.unproxy(app.getProduct()) instanceof CarInsuranceProduct) {
            review.attachAccidentHistory(accidentHistoryClient.fetch(app.getApplicant().getSsn()));
        }
        review.confirm(result, comment, surchargeRate, app.getProduct().getBasePremium());

        if (result == ReviewResult.REJECTED) {
            app.markRejected();
        } else {
            app.markApproved();
            // ADR 0005: 승인 시 같은 트랜잭션에서 계약 생성.
            // ADR 0003: monthlyPremium은 결과 분기 없이 adjustedPremium 한 필드만 읽는다.
            contractRepository.save(new InsuranceContract(
                    app.getApplicant(), app.getProduct(),
                    review.getAdjustedPremium(), LocalDate.now()));
        }
        reviewRepository.save(review);

        notificationSender.send(app.getApplicant().getEmail(), app.getApplicant().getPhone(),
                "가입 심사가 완료되었습니다. 결과: " + result.name()
                        + (result == ReviewResult.CONDITIONAL
                           ? " (조정 보험료 " + review.getAdjustedPremium() + "원)" : ""));
        return review;
    }

    private InsuranceApplication requireApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));
    }

    private InsuranceEmployee requireEmployee(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u instanceof InsuranceEmployee)
                .map(u -> (InsuranceEmployee) u)
                .orElseThrow(() -> new IllegalArgumentException("심사 직원을 찾을 수 없습니다."));
    }
}
