package com.edstem.expense.group.dto;

import java.time.Instant;

public record GroupSummaryResponse(Long id, String name, long memberCount, Instant createdAt) {}
