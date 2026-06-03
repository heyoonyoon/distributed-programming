package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.ApplicationStatus;
import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.review.AccidentHistory;
import com.distribution.insurance.domain.review.EnrollmentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ReviewRepository;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ApplicationRepository applicationRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AccidentHistoryClient accidentHistoryClient;
    private final NotificationSender notificationSender;

    public ReviewService(ApplicationRepository applicationRepository,
                         ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         AccidentHistoryClient accidentHistoryClient,
                         NotificationSender notificationSender) {
        this.applicationRepository = applicationRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.accidentHistoryClient = accidentHistoryClient;
        this.notificationSender = notificationSender;
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
        if (app.getProduct() instanceof CarInsuranceProduct) {
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
        if (app.getProduct() instanceof CarInsuranceProduct) {
            review.attachAccidentHistory(accidentHistoryClient.fetch(app.getApplicant().getSsn()));
        }
        review.confirm(result, comment, surchargeRate, app.getProduct().getBasePremium());

        if (result == ReviewResult.REJECTED) {
            app.markRejected();
        } else {
            app.markApproved();
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
