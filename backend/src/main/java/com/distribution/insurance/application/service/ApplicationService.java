package com.distribution.insurance.application.service;

import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.application.domain.VehicleInfo;
import com.distribution.insurance.product.domain.InsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.application.repository.ApplicationRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.distribution.insurance.common.service.NotificationSender;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository,
                              NotificationSender notificationSender) {
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public InsuranceApplication apply(Long applicantId, Long productId,
                                      VehicleInfo vehicleInfo, MedicalHistory medicalHistory) {
        Policyholder applicant = requirePolicyholder(applicantId);
        InsuranceProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        InsuranceApplication app = new InsuranceApplication(applicant, product, vehicleInfo, medicalHistory);
        applicationRepository.save(app);

        notificationSender.send(applicant.getEmail(), applicant.getPhone(),
                "가입 신청이 접수되었습니다. 접수번호 " + app.getId() + " — 예상 처리 기간 3영업일.");
        return app;
    }

    @Transactional(readOnly = true)
    public List<InsuranceApplication> myApplications(Long applicantId) {
        return applicationRepository.findByApplicantId(applicantId);
    }

    @Transactional
    public void cancel(Long applicantId, Long applicationId) {
        InsuranceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));
        if (!app.getApplicant().getId().equals(applicantId)) {
            throw new IllegalStateException("본인의 신청만 취소할 수 있습니다.");
        }
        app.cancel();
    }

    private Policyholder requirePolicyholder(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u instanceof Policyholder)
                .map(u -> (Policyholder) u)
                .orElseThrow(() -> new IllegalArgumentException("가입자를 찾을 수 없습니다."));
    }
}
