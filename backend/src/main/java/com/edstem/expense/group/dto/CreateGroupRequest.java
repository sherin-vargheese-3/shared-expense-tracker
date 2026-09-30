package com.edstem.expense.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateGroupRequest(
    @Schema(example = "Goa trip") @NotBlank(message = "must not be blank") @Size(max = 100)
        String name,
    @Schema(description = "Initial member names (optional)", example = "[\"Alice\", \"Bob\"]")
        List<@NotBlank(message = "must not be blank") @Size(max = 100) String> members) {

  public List<String> membersOrEmpty() {
    return members == null ? List.of() : members;
  }
}
