package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarAccidentReportRepository extends JpaRepository<CarAccidentReport, Long> {
}
