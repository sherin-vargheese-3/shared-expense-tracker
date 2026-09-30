package com.edstem.expense.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateSettlementRequest(
    @Schema(description = "Member making the payment", example = "2")
        @NotNull(message = "must not be null")
        Long fromMemberId,
    @Schema(description = "Member receiving the payment", example = "1")
        @NotNull(message = "must not be null")
        Long toMemberId,
    @Schema(example = "33.33")
        @NotNull(message = "must not be null")
        @Positive(message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount) {}
