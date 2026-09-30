package com.edstem.expense.expense;

import com.edstem.expense.expense.dto.CreateExpenseRequest;
import com.edstem.expense.expense.dto.ExpenseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@Tag(name = "Expenses", description = "Record and view shared expenses")
public class ExpenseController {

  private final ExpenseService expenseService;

  public ExpenseController(ExpenseService expenseService) {
    this.expenseService = expenseService;
  }

  @PostMapping
  @Operation(
      summary = "Add an expense",
      description =
          "EQUAL splits divide the amount evenly; leftover cents go to the lowest member ids"
              + " first. EXACT splits must list an amount per member that adds up to the total.")
  @ApiResponse(responseCode = "201", description = "Expense recorded")
  public ResponseEntity<ExpenseResponse> createExpense(
      @PathVariable Long groupId, @Valid @RequestBody CreateExpenseRequest request) {
    ExpenseResponse created = expenseService.createExpense(groupId, request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  @Operation(summary = "List a group's expenses, newest first")
  public List<ExpenseResponse> listExpenses(@PathVariable Long groupId) {
    return expenseService.listExpenses(groupId);
  }

  @GetMapping("/{expenseId}")
  @Operation(summary = "Get a single expense")
  public ExpenseResponse getExpense(@PathVariable Long groupId, @PathVariable Long expenseId) {
    return expenseService.getExpense(groupId, expenseId);
  }
}
