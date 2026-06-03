package com.distribution.insurance.repository;

import com.distribution.insurance.domain.review.EnrollmentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<EnrollmentReview, Long> {

    Optional<EnrollmentReview> findByApplicationId(Long applicationId);
}
