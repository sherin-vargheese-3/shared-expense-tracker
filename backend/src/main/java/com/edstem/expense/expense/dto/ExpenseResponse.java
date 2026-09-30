package com.edstem.expense.expense.dto;

import com.edstem.expense.common.Money;
import com.edstem.expense.expense.Expense;
import com.edstem.expense.expense.SplitType;
import com.edstem.expense.group.dto.MemberResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ExpenseResponse(
    Long id,
    String description,
    BigDecimal amount,
    MemberResponse paidBy,
    SplitType splitType,
    Instant createdAt,
    List<SplitResponse> splits) {

  public record SplitResponse(
      Long memberId,
      String memberName,
      @Schema(description = "True if this member has since been removed from the group")
          boolean memberRemoved,
      BigDecimal amount) {}

  public static ExpenseResponse from(Expense expense) {
    List<SplitResponse> splits =
        expense.getSplits().stream()
            .map(
                s ->
                    new SplitResponse(
                        s.getMember().getId(),
                        s.getMember().getName(),
                        !s.getMember().isActive(),
                        Money.toAmount(s.getShareCents())))
            .sorted(Comparator.comparing(SplitResponse::memberId))
            .toList();
    return new ExpenseResponse(
        expense.getId(),
        expense.getDescription(),
        Money.toAmount(expense.getAmountCents()),
        MemberResponse.from(expense.getPaidBy()),
        expense.getSplitType(),
        expense.getCreatedAt(),
        splits);
  }
}
