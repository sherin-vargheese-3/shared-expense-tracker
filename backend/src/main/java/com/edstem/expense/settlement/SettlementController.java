package com.edstem.expense.settlement;

import com.edstem.expense.settlement.dto.CreateSettlementRequest;
import com.edstem.expense.settlement.dto.SettlementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
@Tag(name = "Settlements", description = "Record payments between members")
public class SettlementController {

  private final SettlementService settlementService;

  public SettlementController(SettlementService settlementService) {
    this.settlementService = settlementService;
  }

  @PostMapping
  @Operation(summary = "Record a payment from one member to another")
  @ApiResponse(responseCode = "201", description = "Settlement recorded")
  public ResponseEntity<SettlementResponse> recordSettlement(
      @PathVariable Long groupId, @Valid @RequestBody CreateSettlementRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(settlementService.recordSettlement(groupId, request));
  }

  @GetMapping
  @Operation(summary = "List a group's settlements, newest first")
  public List<SettlementResponse> listSettlements(@PathVariable Long groupId) {
    return settlementService.listSettlements(groupId);
  }
}
