package com.distribution.insurance.contract.repository;

import com.distribution.insurance.contract.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 같은 날 중복 고지 방지(UC16 일일 실행). */
    boolean existsByContractIdAndIssuedAt(Long contractId, LocalDate issuedAt);

    List<Notice> findByContractId(Long contractId);
}
