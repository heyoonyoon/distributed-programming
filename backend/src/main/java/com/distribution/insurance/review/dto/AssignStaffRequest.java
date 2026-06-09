package com.distribution.insurance.review.dto;

import jakarta.validation.constraints.NotNull;

public record AssignStaffRequest(@NotNull Long employeeId) {}
