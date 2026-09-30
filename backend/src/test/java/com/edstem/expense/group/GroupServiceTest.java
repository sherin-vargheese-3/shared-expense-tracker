package com.edstem.expense.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.expense.balance.BalanceService;
import com.edstem.expense.common.ConflictException;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.common.RequestValidationException;
import com.edstem.expense.expense.ExpenseRepository;
import com.edstem.expense.group.dto.AddMemberRequest;
import com.edstem.expense.group.dto.CreateGroupRequest;
import com.edstem.expense.group.dto.MemberResponse;
import com.edstem.expense.settlement.SettlementRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

  @Mock private ExpenseGroupRepository groupRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private ExpenseRepository expenseRepository;
  @Mock private SettlementRepository settlementRepository;
  @Mock private BalanceService balanceService;
  @InjectMocks private GroupService groupService;

  @Test
  void createGroupTrimsNamesAndKeepsMemberOrder() {
    when(groupRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

    var response =
        groupService.createGroup(new CreateGroupRequest("  Trip  ", List.of(" Alice ", "Bob")));

    assertThat(response.name()).isEqualTo("Trip");
    assertThat(response.members()).extracting(MemberResponse::name).containsExactly("Alice", "Bob");
  }

  @Test
  void createGroupWithoutMembersIsAllowed() {
    when(groupRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

    var response = groupService.createGroup(new CreateGroupRequest("Flat", null));

    assertThat(response.members()).isEmpty();
  }

  @Test
  void createGroupRejectsCaseInsensitiveDuplicateMembers() {
    var request = new CreateGroupRequest("Trip", List.of("Alice", "Bob", " alice"));

    assertThatThrownBy(() -> groupService.createGroup(request))
        .isInstanceOfSatisfying(
            RequestValidationException.class,
            ex -> {
              assertThat(ex.errors()).hasSize(1);
              assertThat(ex.errors().getFirst().field()).isEqualTo("members[2]");
              assertThat(ex.errors().getFirst().message()).contains("alice");
            });
    verify(groupRepository, never()).saveAndFlush(any());
  }

  @Test
  void getGroupThrowsNotFoundForUnknownId() {
    when(groupRepository.findWithMembersById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> groupService.getGroup(99L))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Group 99 not found");
  }

  @Test
  void addMemberSavesTrimmedName() {
    var group = new ExpenseGroup("Trip");
    when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
    when(memberRepository.existsByGroupIdAndNameIgnoreCaseAndRemovedAtIsNull(1L, "Charlie"))
        .thenReturn(false);
    when(memberRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

    var response = groupService.addMember(1L, new AddMemberRequest("  Charlie "));

    var saved = ArgumentCaptor.forClass(Member.class);
    verify(memberRepository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getGroup()).isSameAs(group);
    assertThat(response.name()).isEqualTo("Charlie");
  }

  @Test
  void addMemberRejectsExistingName() {
    when(groupRepository.findById(1L)).thenReturn(Optional.of(new ExpenseGroup("Trip")));
    when(memberRepository.existsByGroupIdAndNameIgnoreCaseAndRemovedAtIsNull(1L, "alice"))
        .thenReturn(true);

    assertThatThrownBy(() -> groupService.addMember(1L, new AddMemberRequest("alice")))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("alice");
    verify(memberRepository, never()).saveAndFlush(any());
  }

  @Test
  void addMemberToUnknownGroupThrowsNotFound() {
    when(groupRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> groupService.addMember(5L, new AddMemberRequest("Zed")))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void listMembersOfUnknownGroupThrowsNotFound() {
    when(groupRepository.existsById(5L)).thenReturn(false);

    assertThatThrownBy(() -> groupService.listMembers(5L)).isInstanceOf(NotFoundException.class);
  }

  private Member activeMember(long id, String name) {
    Member member = new ExpenseGroup("Trip").addMember(name);
    ReflectionTestUtils.setField(member, "id", id);
    when(groupRepository.existsById(1L)).thenReturn(true);
    when(memberRepository.findByIdAndGroupIdAndRemovedAtIsNull(id, 1L))
        .thenReturn(Optional.of(member));
    return member;
  }

  @Test
  void removeMemberWithoutHistoryDeletesThem() {
    Member dana = activeMember(4L, "Dana");
    when(balanceService.balanceCents(1L, 4L)).thenReturn(0L);

    groupService.removeMember(1L, 4L);

    verify(memberRepository).delete(dana);
    assertThat(dana.isActive()).isTrue();
  }

  @Test
  void removeMemberWithHistorySoftRemovesThem() {
    Member dana = activeMember(4L, "Dana");
    when(balanceService.balanceCents(1L, 4L)).thenReturn(0L);
    when(expenseRepository.isMemberInvolved(4L)).thenReturn(true);

    groupService.removeMember(1L, 4L);

    assertThat(dana.isActive()).isFalse();
    verify(memberRepository, never()).delete(any());
  }

  @Test
  void removeMemberWhoOnlySettledIsAlsoSoftRemoved() {
    Member dana = activeMember(4L, "Dana");
    when(balanceService.balanceCents(1L, 4L)).thenReturn(0L);
    when(settlementRepository.existsByFromMemberIdOrToMemberId(4L, 4L)).thenReturn(true);

    groupService.removeMember(1L, 4L);

    assertThat(dana.isActive()).isFalse();
  }

  @Test
  void removeMemberWithOutstandingBalanceIsRejected() {
    Member dana = activeMember(4L, "Dana");
    when(balanceService.balanceCents(1L, 4L)).thenReturn(-1250L);

    assertThatThrownBy(() -> groupService.removeMember(1L, 4L))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Dana still owes 12.50. Settle up before removing them.");
    assertThat(dana.isActive()).isTrue();
    verify(memberRepository, never()).delete(any());
  }

  @Test
  void removeMemberWhoIsOwedMoneyIsRejected() {
    activeMember(4L, "Dana");
    when(balanceService.balanceCents(1L, 4L)).thenReturn(300L);

    assertThatThrownBy(() -> groupService.removeMember(1L, 4L))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Dana is still owed 3.00. Settle up before removing them.");
  }

  @Test
  void removeUnknownMemberIsNotFound() {
    when(groupRepository.existsById(1L)).thenReturn(true);
    when(memberRepository.findByIdAndGroupIdAndRemovedAtIsNull(9L, 1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> groupService.removeMember(1L, 9L))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Member 9 not found");
  }

  @Test
  void deleteGroupRemovesDependentsBeforeTheGroup() {
    when(groupRepository.existsById(1L)).thenReturn(true);

    groupService.deleteGroup(1L);

    var order = inOrder(expenseRepository, settlementRepository, groupRepository);
    order.verify(expenseRepository).deleteAllByGroupId(1L);
    order.verify(settlementRepository).deleteAllByGroupId(1L);
    order.verify(groupRepository).deleteGroupById(1L);
  }

  @Test
  void deleteUnknownGroupIsNotFound() {
    when(groupRepository.existsById(9L)).thenReturn(false);

    assertThatThrownBy(() -> groupService.deleteGroup(9L)).isInstanceOf(NotFoundException.class);
    verify(expenseRepository, never()).deleteAllByGroupId(any());
  }
}
