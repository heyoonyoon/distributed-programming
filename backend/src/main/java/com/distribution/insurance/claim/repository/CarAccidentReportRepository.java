package com.distribution.insurance.claim.repository;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarAccidentReportRepository extends JpaRepository<CarAccidentReport, Long> {
}
