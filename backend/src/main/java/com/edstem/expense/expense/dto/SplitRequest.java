package com.edstem.expense.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SplitRequest(
    @Schema(example = "1") @NotNull(message = "must not be null") Long memberId,
    @Schema(
            description = "Required for EXACT splits; must be omitted for EQUAL splits",
            example = "25.50")
        @Positive(message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount) {}
