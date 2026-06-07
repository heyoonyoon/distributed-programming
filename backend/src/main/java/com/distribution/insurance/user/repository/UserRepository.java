package com.distribution.insurance.user.repository;

import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /** 30일 초과 연체 발생 시 별도 알림 대상(UC16 A1). */
    @Query("select e from InsuranceEmployee e")
    List<InsuranceEmployee> findAllEmployees();
}
