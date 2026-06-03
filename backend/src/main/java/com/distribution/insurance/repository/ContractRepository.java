package com.distribution.insurance.repository;

import com.distribution.insurance.domain.contract.InsuranceContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<InsuranceContract, Long> {

    /** 가입자 본인 계약 목록(UC08). */
    List<InsuranceContract> findByPolicyholderId(Long policyholderId);
}
