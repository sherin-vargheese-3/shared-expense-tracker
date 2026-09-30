package com.edstem.expense.expense;

import com.edstem.expense.common.FieldErrorDto;
import com.edstem.expense.common.Money;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.common.RequestValidationException;
import com.edstem.expense.expense.dto.CreateExpenseRequest;
import com.edstem.expense.expense.dto.ExpenseResponse;
import com.edstem.expense.expense.dto.SplitRequest;
import com.edstem.expense.group.ExpenseGroup;
import com.edstem.expense.group.ExpenseGroupRepository;
import com.edstem.expense.group.Member;
import com.edstem.expense.group.MemberRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

  private final ExpenseRepository expenseRepository;
  private final ExpenseGroupRepository groupRepository;
  private final MemberRepository memberRepository;

  public ExpenseService(
      ExpenseRepository expenseRepository,
      ExpenseGroupRepository groupRepository,
      MemberRepository memberRepository) {
    this.expenseRepository = expenseRepository;
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
  }

  @Transactional
  public ExpenseResponse createExpense(Long groupId, CreateExpenseRequest request) {
    ExpenseGroup group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> NotFoundException.of("Group", groupId));
    Map<Long, Member> members =
        memberRepository.findActiveByGroupId(groupId).stream()
            .collect(Collectors.toMap(Member::getId, Function.identity()));

    long amountCents = Money.toCents(request.amount());
    Map<Long, Long> shares = resolveShares(request, amountCents, members);

    Expense expense =
        new Expense(
            group,
            request.description().strip(),
            amountCents,
            members.get(request.paidBy()),
            request.splitType());
    shares.forEach((memberId, share) -> expense.addSplit(members.get(memberId), share));

    return ExpenseResponse.from(expenseRepository.saveAndFlush(expense));
  }

  public List<ExpenseResponse> listExpenses(Long groupId) {
    requireGroup(groupId);
    return expenseRepository.findAllWithDetailsByGroupId(groupId).stream()
        .map(ExpenseResponse::from)
        .toList();
  }

  public ExpenseResponse getExpense(Long groupId, Long expenseId) {
    requireGroup(groupId);
    return expenseRepository
        .findWithDetailsByIdAndGroupId(expenseId, groupId)
        .map(ExpenseResponse::from)
        .orElseThrow(() -> NotFoundException.of("Expense", expenseId));
  }

  /**
   * Validates payer and participants against the group and computes each participant's share in
   * cents. All problems are collected and reported together.
   */
  private static Map<Long, Long> resolveShares(
      CreateExpenseRequest request, long amountCents, Map<Long, Member> members) {
    List<FieldErrorDto> errors = new ArrayList<>();

    if (!members.containsKey(request.paidBy())) {
      errors.add(new FieldErrorDto("paidBy", notInGroup(request.paidBy())));
    }

    List<SplitRequest> splits = request.splits();
    Set<Long> seen = new HashSet<>();
    for (int i = 0; i < splits.size(); i++) {
      SplitRequest split = splits.get(i);
      String field = "splits[%d]".formatted(i);
      if (!members.containsKey(split.memberId())) {
        errors.add(new FieldErrorDto(field + ".memberId", notInGroup(split.memberId())));
      } else if (!seen.add(split.memberId())) {
        errors.add(
            new FieldErrorDto(
                field + ".memberId",
                "member %d is listed more than once".formatted(split.memberId())));
      }
      if (request.splitType() == SplitType.EQUAL && split.amount() != null) {
        errors.add(new FieldErrorDto(field + ".amount", "must be omitted for EQUAL splits"));
      }
      if (request.splitType() == SplitType.EXACT && split.amount() == null) {
        errors.add(new FieldErrorDto(field + ".amount", "is required for EXACT splits"));
      }
    }

    if (!errors.isEmpty()) {
      throw new RequestValidationException(errors);
    }

    return switch (request.splitType()) {
      case EQUAL -> SplitCalculator.equalSplit(amountCents, seen);
      case EXACT -> exactShares(splits, amountCents);
    };
  }

  private static Map<Long, Long> exactShares(List<SplitRequest> splits, long amountCents) {
    Map<Long, Long> shares = new LinkedHashMap<>();
    long total = 0;
    for (SplitRequest split : splits) {
      long share = Money.toCents(split.amount());
      shares.put(split.memberId(), share);
      total = Math.addExact(total, share);
    }
    if (total != amountCents) {
      throw new RequestValidationException(
          "splits",
          "split amounts add up to %s but the expense amount is %s"
              .formatted(Money.toAmount(total), Money.toAmount(amountCents)));
    }
    return shares;
  }

  private static String notInGroup(Long memberId) {
    return "member %d is not in this group".formatted(memberId);
  }

  private void requireGroup(Long groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw NotFoundException.of("Group", groupId);
    }
  }
}
