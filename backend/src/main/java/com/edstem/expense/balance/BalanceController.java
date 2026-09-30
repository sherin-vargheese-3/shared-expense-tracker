package com.edstem.expense.balance;

import com.edstem.expense.balance.dto.BalanceResponse;
import com.edstem.expense.balance.dto.SuggestedPaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}")
@Tag(name = "Balances", description = "Who owes whom, and how to settle up")
public class BalanceController {

  private final BalanceService balanceService;

  public BalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  @GetMapping("/balances")
  @Operation(
      summary = "Current balance of every member",
      description = "Positive balances are owed money; negative balances owe. They sum to zero.")
  public List<BalanceResponse> getBalances(@PathVariable Long groupId) {
    return balanceService.getBalances(groupId);
  }

  @GetMapping("/suggested-payments")
  @Operation(
      summary = "Fewest payments that settle all debts",
      description =
          "Exact minimum for up to 20 members with non-zero balances; greedy (at most n-1"
              + " payments) above that.")
  public List<SuggestedPaymentResponse> getSuggestedPayments(@PathVariable Long groupId) {
    return balanceService.getSuggestedPayments(groupId);
  }
}
