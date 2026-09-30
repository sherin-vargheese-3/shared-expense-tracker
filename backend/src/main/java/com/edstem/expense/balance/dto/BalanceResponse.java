package com.edstem.expense.balance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record BalanceResponse(
    Long memberId,
    String memberName,
    @Schema(description = "Positive: the member is owed money. Negative: the member owes.")
        BigDecimal balance) {}
