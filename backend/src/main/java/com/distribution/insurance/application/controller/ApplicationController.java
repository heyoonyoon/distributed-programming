package com.distribution.insurance.application.controller;

import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.application.service.ApplicationService;
import com.distribution.insurance.application.dto.ApplicationResponse;
import com.distribution.insurance.application.dto.ApplicationSummaryResponse;
import com.distribution.insurance.application.dto.CreateApplicationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@AuthenticationPrincipal Long userId,
                                     @Valid @RequestBody CreateApplicationRequest request) {
        InsuranceApplication app = applicationService.apply(
                userId, request.productId(), request.toVehicleInfo(), request.toMedicalHistory());
        return ApplicationResponse.from(app);
    }

    @GetMapping("/me")
    public List<ApplicationSummaryResponse> myApplications(@AuthenticationPrincipal Long userId) {
        return applicationService.myApplications(userId).stream()
                .map(ApplicationSummaryResponse::from)
                .toList();
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        applicationService.cancel(userId, id);
    }
}
