package com.edstem.expense.group.dto;

import com.edstem.expense.group.Member;

public record MemberResponse(Long id, String name) {

  public static MemberResponse from(Member member) {
    return new MemberResponse(member.getId(), member.getName());
  }
}
