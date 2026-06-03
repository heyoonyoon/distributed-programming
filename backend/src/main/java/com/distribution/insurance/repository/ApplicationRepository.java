package com.distribution.insurance.repository;

import com.distribution.insurance.domain.application.ApplicationStatus;
import com.distribution.insurance.domain.application.InsuranceApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<InsuranceApplication, Long> {

    List<InsuranceApplication> findByApplicantId(Long applicantId);

    List<InsuranceApplication> findByStatus(ApplicationStatus status);
}
