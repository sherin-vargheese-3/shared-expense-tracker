package com.edstem.expense.settlement.dto;

import com.edstem.expense.common.Money;
import com.edstem.expense.group.dto.MemberResponse;
import com.edstem.expense.settlement.Settlement;
import java.math.BigDecimal;
import java.time.Instant;

public record SettlementResponse(
    Long id, MemberResponse from, MemberResponse to, BigDecimal amount, Instant createdAt) {

  public static SettlementResponse from(Settlement settlement) {
    return new SettlementResponse(
        settlement.getId(),
        MemberResponse.from(settlement.getFromMember()),
        MemberResponse.from(settlement.getToMember()),
        Money.toAmount(settlement.getAmountCents()),
        settlement.getCreatedAt());
  }
}
