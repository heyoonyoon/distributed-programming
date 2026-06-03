package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.ClaimAttachment;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.service.AttachmentValidator;
import com.distribution.insurance.service.CarAccidentService;
import com.distribution.insurance.service.ClaimService;
import com.distribution.insurance.service.FileStorage;
import com.distribution.insurance.web.dto.CarAccidentResultResponse;
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
    private final CarAccidentService carAccidentService;

    public ClaimController(ClaimService claimService, AttachmentValidator attachmentValidator,
                           FileStorage fileStorage, CarAccidentService carAccidentService) {
        this.claimService = claimService;
        this.attachmentValidator = attachmentValidator;
        this.fileStorage = fileStorage;
        this.carAccidentService = carAccidentService;
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

    @PostMapping(path = "/car-accidents", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public CarAccidentResultResponse reportCarAccident(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long contractId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accidentDate,
            @RequestParam String accidentLocation,
            @RequestParam String accidentType,
            @RequestParam String vehicleNumber,
            @RequestParam boolean hasInjury,
            @RequestParam int injuredCount,
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
            CarAccidentReport report = carAccidentService.report(
                    userId, contractId, accidentDate, accidentLocation, accidentType,
                    vehicleNumber, hasInjury, injuredCount, metas);
            return CarAccidentResultResponse.from(report);
        } catch (RuntimeException e) {
            metas.forEach(m -> fileStorage.delete(m.getStoredPath()));
            throw e;
        }
    }
}
