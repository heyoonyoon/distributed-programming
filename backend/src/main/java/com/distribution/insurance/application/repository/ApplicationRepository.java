package com.distribution.insurance.application.repository;

import com.distribution.insurance.application.domain.ApplicationStatus;
import com.distribution.insurance.application.domain.InsuranceApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<InsuranceApplication, Long> {

    List<InsuranceApplication> findByApplicantId(Long applicantId);

    List<InsuranceApplication> findByStatus(ApplicationStatus status);
}
