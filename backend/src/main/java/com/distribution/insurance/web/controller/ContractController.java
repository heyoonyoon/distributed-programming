package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.service.ContractService;
import com.distribution.insurance.web.dto.ContractDetailResponse;
import com.distribution.insurance.web.dto.ContractSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public List<ContractSummaryResponse> myContracts(@AuthenticationPrincipal Long userId) {
        return contractService.myContracts(userId).stream()
                .map(ContractSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ContractDetailResponse detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        InsuranceContract c = contractService.detail(userId, id);
        return ContractDetailResponse.from(c);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        byte[] pdf = contractService.generatePdf(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract-" + id + ".txt\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(pdf);
    }
}
