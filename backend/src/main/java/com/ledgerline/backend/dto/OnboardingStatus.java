package com.ledgerline.backend.dto;

import java.util.List;

public record OnboardingStatus(String fullName, String email, String status, String rejectionReason,
                               List<AccountRequestView> requests) {}