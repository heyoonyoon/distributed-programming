package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.service.AttachmentValidator;
import com.distribution.insurance.service.ClaimService;
import com.distribution.insurance.service.FileStorage;
import com.distribution.insurance.web.dto.HealthClaimResultResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final AttachmentValidator attachmentValidator;
    private final FileStorage fileStorage;

    public ClaimController(ClaimService claimService, AttachmentValidator attachmentValidator,
                           FileStorage fileStorage) {
        this.claimService = claimService;
        this.attachmentValidator = attachmentValidator;
        this.fileStorage = fileStorage;
    }

    @PostMapping(path = "/health", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public HealthClaimResultResponse fileHealthClaim(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long contractId,
            @RequestParam String hospitalName,
            @RequestParam String diagnosisCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate treatmentDate,
            @RequestParam int requestAmount,
            @RequestParam int receiptAmount,
            @RequestParam(required = false) MultipartFile[] attachments) {

        List<ClaimAttachment> metas = new ArrayList<>();
        if (attachments != null) {
            for (MultipartFile f : attachments) {
                if (f.isEmpty()) continue;
                attachmentValidator.validate(f);
                metas.add(fileStorage.store(userId, f));
            }
        }

        try {
            HealthInsuranceClaim claim = claimService.fileHealthClaim(
                    userId, contractId, hospitalName, diagnosisCode, treatmentDate,
                    requestAmount, receiptAmount, metas);
            return HealthClaimResultResponse.from(claim);
        } catch (Exception e) {
            // Fix 4: cleanup orphaned uploaded files when service rejects the request
            metas.forEach(meta -> fileStorage.delete(meta.getStoredPath()));
            throw e;
        }
    }
}
