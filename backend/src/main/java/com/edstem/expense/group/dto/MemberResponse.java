package com.edstem.expense.group.dto;

import com.edstem.expense.group.Member;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberResponse(
    Long id,
    String name,
    @Schema(
            description =
                "True for a former member who still appears in past expenses or payments. A new"
                    + " member may since have been added with the same name.")
        boolean removed) {

  public static MemberResponse from(Member member) {
    return new MemberResponse(member.getId(), member.getName(), !member.isActive());
  }
}
