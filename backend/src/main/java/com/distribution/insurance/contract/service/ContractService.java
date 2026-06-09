package com.distribution.insurance.contract.service;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.contract.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<InsuranceContract> myContracts(Long policyholderId) {
        return contractRepository.findByPolicyholderId(policyholderId);
    }

    @Transactional(readOnly = true)
    public InsuranceContract detail(Long policyholderId, Long contractId) {
        return requireOwned(policyholderId, contractId);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long policyholderId, Long contractId) {
        return requireOwned(policyholderId, contractId).generatePdf();
    }

    /** 존재하지 않으면 404(IllegalArgumentException), 타인 계약이면 403(IllegalStateException). */
    private InsuranceContract requireOwned(Long policyholderId, Long contractId) {
        InsuranceContract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!c.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인의 계약만 조회할 수 있습니다.");
        }
        return c;
    }
}
