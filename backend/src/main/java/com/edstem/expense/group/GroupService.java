package com.edstem.expense.group;

import com.edstem.expense.balance.BalanceService;
import com.edstem.expense.common.ConflictException;
import com.edstem.expense.common.Money;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.common.RequestValidationException;
import com.edstem.expense.expense.ExpenseRepository;
import com.edstem.expense.group.dto.AddMemberRequest;
import com.edstem.expense.group.dto.CreateGroupRequest;
import com.edstem.expense.group.dto.GroupResponse;
import com.edstem.expense.group.dto.GroupSummaryResponse;
import com.edstem.expense.group.dto.MemberResponse;
import com.edstem.expense.settlement.SettlementRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GroupService {

  private final ExpenseGroupRepository groupRepository;
  private final MemberRepository memberRepository;
  private final ExpenseRepository expenseRepository;
  private final SettlementRepository settlementRepository;
  private final BalanceService balanceService;

  public GroupService(
      ExpenseGroupRepository groupRepository,
      MemberRepository memberRepository,
      ExpenseRepository expenseRepository,
      SettlementRepository settlementRepository,
      BalanceService balanceService) {
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
    this.expenseRepository = expenseRepository;
    this.settlementRepository = settlementRepository;
    this.balanceService = balanceService;
  }

  @Transactional
  public GroupResponse createGroup(CreateGroupRequest request) {
    ExpenseGroup group = new ExpenseGroup(request.name().strip());

    Set<String> seen = new HashSet<>();
    List<String> memberNames = request.membersOrEmpty();
    for (int i = 0; i < memberNames.size(); i++) {
      String name = memberNames.get(i).strip();
      if (!seen.add(normalize(name))) {
        throw new RequestValidationException(
            "members[%d]".formatted(i), "duplicate member name '%s'".formatted(name));
      }
      group.addMember(name);
    }

    return GroupResponse.from(groupRepository.saveAndFlush(group));
  }

  public List<GroupSummaryResponse> listGroups() {
    return groupRepository.findAllSummaries();
  }

  public GroupResponse getGroup(Long groupId) {
    return groupRepository
        .findWithMembersById(groupId)
        .map(GroupResponse::from)
        .orElseThrow(() -> NotFoundException.of("Group", groupId));
  }

  @Transactional
  public MemberResponse addMember(Long groupId, AddMemberRequest request) {
    ExpenseGroup group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> NotFoundException.of("Group", groupId));

    String name = request.name().strip();
    if (memberRepository.existsByGroupIdAndNameIgnoreCaseAndRemovedAtIsNull(groupId, name)) {
      throw new ConflictException(
          "A member named '%s' already exists in this group".formatted(name));
    }

    return MemberResponse.from(memberRepository.saveAndFlush(group.addMember(name)));
  }

  public List<MemberResponse> listMembers(Long groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw NotFoundException.of("Group", groupId);
    }
    return memberRepository.findActiveByGroupId(groupId).stream()
        .map(MemberResponse::from)
        .toList();
  }

  /**
   * Deletes the group together with its members, expenses and settlements. Rows are deleted in
   * dependency order: Postgres checks foreign keys after each cascaded delete, so deleting the
   * group first would fail on members still referenced by expense splits.
   */
  @Transactional
  public void deleteGroup(Long groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw NotFoundException.of("Group", groupId);
    }
    expenseRepository.deleteAllByGroupId(groupId);
    settlementRepository.deleteAllByGroupId(groupId);
    groupRepository.deleteGroupById(groupId);
  }

  /**
   * Removes a member whose balance is settled. Members with no expenses or settlements are deleted
   * outright; others are soft-removed so past expenses keep showing who paid and who shared.
   *
   * @throws ConflictException if the member still owes or is owed money
   */
  @Transactional
  public void removeMember(Long groupId, Long memberId) {
    if (!groupRepository.existsById(groupId)) {
      throw NotFoundException.of("Group", groupId);
    }
    Member member =
        memberRepository
            .findByIdAndGroupIdAndRemovedAtIsNull(memberId, groupId)
            .orElseThrow(() -> NotFoundException.of("Member", memberId));

    long balance = balanceService.balanceCents(groupId, memberId);
    if (balance != 0) {
      String position =
          balance < 0
              ? "still owes " + Money.toAmount(-balance)
              : "is still owed " + Money.toAmount(balance);
      throw new ConflictException(
          "%s %s. Settle up before removing them.".formatted(member.getName(), position));
    }

    boolean hasHistory =
        expenseRepository.isMemberInvolved(memberId)
            || settlementRepository.existsByFromMemberIdOrToMemberId(memberId, memberId);
    if (hasHistory) {
      member.markRemoved();
    } else {
      member.getGroup().getMembers().remove(member);
      memberRepository.delete(member);
    }
  }

  private static String normalize(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
