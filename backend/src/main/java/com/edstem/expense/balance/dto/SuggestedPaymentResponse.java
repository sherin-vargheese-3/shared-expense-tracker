package com.edstem.expense.balance.dto;

import java.math.BigDecimal;

public record SuggestedPaymentResponse(
    Long fromMemberId,
    String fromMemberName,
    Long toMemberId,
    String toMemberName,
    BigDecimal amount) {}
