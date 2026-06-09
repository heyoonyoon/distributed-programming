package com.distribution.insurance.review.repository;

import com.distribution.insurance.review.domain.EnrollmentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<EnrollmentReview, Long> {

    Optional<EnrollmentReview> findByApplicationId(Long applicationId);
}
