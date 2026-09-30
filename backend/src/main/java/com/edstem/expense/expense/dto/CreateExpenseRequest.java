package com.edstem.expense.expense.dto;

import com.edstem.expense.expense.SplitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateExpenseRequest(
    @Schema(example = "Dinner at the beach shack")
        @NotBlank(message = "must not be blank")
        @Size(max = 255)
        String description,
    @Schema(example = "100.00")
        @NotNull(message = "must not be null")
        @Positive(message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount,
    @Schema(description = "Id of the member who paid", example = "1")
        @NotNull(message = "must not be null")
        Long paidBy,
    @NotNull(message = "must not be null") SplitType splitType,
    @Schema(description = "Members sharing the expense")
        @NotEmpty(message = "must contain at least one member")
        List<@NotNull(message = "must not be null") @Valid SplitRequest> splits) {}
