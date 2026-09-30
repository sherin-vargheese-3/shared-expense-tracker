package com.edstem.expense.group.dto;

import com.edstem.expense.group.ExpenseGroup;
import com.edstem.expense.group.Member;
import java.time.Instant;
import java.util.List;

public record GroupResponse(Long id, String name, Instant createdAt, List<MemberResponse> members) {

  public static GroupResponse from(ExpenseGroup group) {
    return new GroupResponse(
        group.getId(),
        group.getName(),
        group.getCreatedAt(),
        group.getMembers().stream().filter(Member::isActive).map(MemberResponse::from).toList());
  }
}
