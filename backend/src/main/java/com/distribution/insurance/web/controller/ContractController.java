package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.Payment;
import com.distribution.insurance.service.BillingService;
import com.distribution.insurance.service.BillingService.PayOutcome;
import com.distribution.insurance.service.ContractService;
import com.distribution.insurance.web.dto.AutoDebitRequest;
import com.distribution.insurance.web.dto.ContractDetailResponse;
import com.distribution.insurance.web.dto.ContractSummaryResponse;
import com.distribution.insurance.web.dto.PayableResponse;
import com.distribution.insurance.web.dto.PaymentRequest;
import com.distribution.insurance.web.dto.PaymentResultResponse;
import com.distribution.insurance.web.dto.UnpaidResponse;
import jakarta.validation.Valid;
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
    private final BillingService billingService;

    public ContractController(ContractService contractService, BillingService billingService) {
        this.contractService = contractService;
        this.billingService = billingService;
    }

    @GetMapping
    public List<ContractSummaryResponse> myContracts(@AuthenticationPrincipal Long userId) {
        return contractService.myContracts(userId).stream()
                .map(ContractSummaryResponse::from)
                .toList();
    }

    // 고정경로 핸들러는 /{id} 보다 위에 선언(라우팅 충돌 방지).
    @GetMapping("/unpaid")
    public List<UnpaidResponse> myUnpaid(@AuthenticationPrincipal Long userId) {
        return billingService.myOverdue(userId).stream().map(UnpaidResponse::from).toList();
    }

    @GetMapping("/payable")
    public List<PayableResponse> myPayable(@AuthenticationPrincipal Long userId) {
        return billingService.myPayable(userId).stream().map(PayableResponse::from).toList();
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

    @GetMapping("/{id}/unpaid")
    public UnpaidResponse unpaidDetail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return UnpaidResponse.from(billingService.unpaidDetail(userId, id));
    }

    @PostMapping("/{id}/payments")
    public PaymentResultResponse pay(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                     @Valid @RequestBody PaymentRequest request) {
        PayOutcome outcome = billingService.pay(userId, id, request.method(), request.paymentInfo());
        Payment p = outcome.payment();
        return outcome.failureReason() == null
                ? PaymentResultResponse.success(p)
                : PaymentResultResponse.failed(p, outcome.failureReason());
    }

    @PostMapping("/{id}/auto-debit")
    public void registerAutoDebit(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @Valid @RequestBody AutoDebitRequest request) {
        billingService.registerAutoDebit(userId, id, request.account(), request.withdrawalDay());
    }
}
