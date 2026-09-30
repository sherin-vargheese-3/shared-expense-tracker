package com.edstem.expense.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddMemberRequest(
    @Schema(example = "Charlie") @NotBlank(message = "must not be blank") @Size(max = 100)
        String name) {}
