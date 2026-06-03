package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByContractId(Long contractId);

    List<Claim> findByContractPolicyholderId(Long policyholderId);
}
