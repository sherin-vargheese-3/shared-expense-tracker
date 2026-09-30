package com.edstem.expense.balance;

import com.edstem.expense.balance.dto.BalanceResponse;
import com.edstem.expense.balance.dto.SuggestedPaymentResponse;
import com.edstem.expense.common.Money;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.expense.ExpenseRepository;
import com.edstem.expense.group.ExpenseGroupRepository;
import com.edstem.expense.group.Member;
import com.edstem.expense.group.MemberRepository;
import com.edstem.expense.settlement.SettlementRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derives balances from expenses and settlements on every read; nothing is stored. */
@Service
@Transactional(readOnly = true)
public class BalanceService {

  private final ExpenseGroupRepository groupRepository;
  private final MemberRepository memberRepository;
  private final ExpenseRepository expenseRepository;
  private final SettlementRepository settlementRepository;

  public BalanceService(
      ExpenseGroupRepository groupRepository,
      MemberRepository memberRepository,
      ExpenseRepository expenseRepository,
      SettlementRepository settlementRepository) {
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
    this.expenseRepository = expenseRepository;
    this.settlementRepository = settlementRepository;
  }

  public List<BalanceResponse> getBalances(Long groupId) {
    Map<Long, Member> members = loadMembers(groupId);
    return netBalances(groupId, members).entrySet().stream()
        .filter(e -> members.get(e.getKey()).isActive())
        .map(
            e ->
                new BalanceResponse(
                    e.getKey(), members.get(e.getKey()).getName(), Money.toAmount(e.getValue())))
        .toList();
  }

  public List<SuggestedPaymentResponse> getSuggestedPayments(Long groupId) {
    Map<Long, Member> members = loadMembers(groupId);
    return SettlementPlanner.plan(netBalances(groupId, members)).stream()
        .map(
            t ->
                new SuggestedPaymentResponse(
                    t.fromMemberId(),
                    members.get(t.fromMemberId()).getName(),
                    t.toMemberId(),
                    members.get(t.toMemberId()).getName(),
                    Money.toAmount(t.amountCents())))
        .toList();
  }

  /** Net balance of one member in cents; positive means they are owed money. */
  public long balanceCents(Long groupId, Long memberId) {
    return netBalances(groupId, loadMembers(groupId)).getOrDefault(memberId, 0L);
  }

  private Map<Long, Long> netBalances(Long groupId, Map<Long, Member> members) {
    return BalanceCalculator.netBalances(
        members.keySet(),
        expenseRepository.sumPaidByMember(groupId),
        expenseRepository.sumOwedByMember(groupId),
        settlementRepository.sumSentByMember(groupId),
        settlementRepository.sumReceivedByMember(groupId));
  }

  /**
   * All members in id order, including removed ones: their historical expenses still count toward
   * the zero-sum check (their own net is zero, since removal requires a settled balance). Throws
   * 404 if the group does not exist.
   */
  private Map<Long, Member> loadMembers(Long groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw NotFoundException.of("Group", groupId);
    }
    return memberRepository.findByGroupIdOrderById(groupId).stream()
        .collect(
            Collectors.toMap(
                Member::getId,
                Function.identity(),
                (a, b) -> a,
                LinkedHashMap::new));
  }
}
