package com.edstem.expense.settlement;

import com.edstem.expense.common.FieldErrorDto;
import com.edstem.expense.common.Money;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.common.RequestValidationException;
import com.edstem.expense.group.ExpenseGroup;
import com.edstem.expense.group.ExpenseGroupRepository;
import com.edstem.expense.group.Member;
import com.edstem.expense.group.MemberRepository;
import com.edstem.expense.settlement.dto.CreateSettlementRequest;
import com.edstem.expense.settlement.dto.SettlementResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SettlementService {

  private final SettlementRepository settlementRepository;
  private final ExpenseGroupRepository groupRepository;
  private final MemberRepository memberRepository;

  public SettlementService(
      SettlementRepository settlementRepository,
      ExpenseGroupRepository groupRepository,
      MemberRepository memberRepository) {
    this.settlementRepository = settlementRepository;
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
  }

  /**
   * Records a payment. Paying more than is owed is allowed: it simply flips who owes whom, and
   * balances still sum to zero.
   */
  @Transactional
  public SettlementResponse recordSettlement(Long groupId, CreateSettlementRequest request) {
    ExpenseGroup group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> NotFoundException.of("Group", groupId));
    Map<Long, Member> members =
        memberRepository.findActiveByGroupId(groupId).stream()
            .collect(Collectors.toMap(Member::getId, Function.identity()));

    List<FieldErrorDto> errors = new ArrayList<>();
    if (!members.containsKey(request.fromMemberId())) {
      errors.add(new FieldErrorDto("fromMemberId", notInGroup(request.fromMemberId())));
    }
    if (!members.containsKey(request.toMemberId())) {
      errors.add(new FieldErrorDto("toMemberId", notInGroup(request.toMemberId())));
    }
    if (request.fromMemberId().equals(request.toMemberId())) {
      errors.add(new FieldErrorDto("toMemberId", "must be different from fromMemberId"));
    }
    if (!errors.isEmpty()) {
      throw new RequestValidationException(errors);
    }

    Settlement settlement =
        new Settlement(
            group,
            members.get(request.fromMemberId()),
            members.get(request.toMemberId()),
            Money.toCents(request.amount()));
    return SettlementResponse.from(settlementRepository.saveAndFlush(settlement));
  }

  public List<SettlementResponse> listSettlements(Long groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw NotFoundException.of("Group", groupId);
    }
    return settlementRepository.findAllWithMembersByGroupId(groupId).stream()
        .map(SettlementResponse::from)
        .toList();
  }

  private static String notInGroup(Long memberId) {
    return "member %d is not in this group".formatted(memberId);
  }
}
