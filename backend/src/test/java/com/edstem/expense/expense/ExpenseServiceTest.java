package com.edstem.expense.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.expense.common.FieldErrorDto;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.common.RequestValidationException;
import com.edstem.expense.expense.dto.CreateExpenseRequest;
import com.edstem.expense.expense.dto.ExpenseResponse.SplitResponse;
import com.edstem.expense.expense.dto.SplitRequest;
import com.edstem.expense.group.ExpenseGroup;
import com.edstem.expense.group.ExpenseGroupRepository;
import com.edstem.expense.group.Member;
import com.edstem.expense.group.MemberRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseServiceTest {

  private static final long GROUP_ID = 1L;

  @Mock private ExpenseRepository expenseRepository;
  @Mock private ExpenseGroupRepository groupRepository;
  @Mock private MemberRepository memberRepository;
  @InjectMocks private ExpenseService expenseService;

  @BeforeEach
  void setUp() {
    ExpenseGroup group = new ExpenseGroup("Trip");
    Member alice = withId(group.addMember("Alice"), 1L);
    Member bob = withId(group.addMember("Bob"), 2L);
    Member carol = withId(group.addMember("Carol"), 3L);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
    when(memberRepository.findActiveByGroupId(GROUP_ID))
        .thenReturn(List.of(alice, bob, carol));
    when(expenseRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private static Member withId(Member member, long id) {
    ReflectionTestUtils.setField(member, "id", id);
    return member;
  }

  private static CreateExpenseRequest request(
      String amount, long paidBy, SplitType type, SplitRequest... splits) {
    return new CreateExpenseRequest(
        "Dinner", new BigDecimal(amount), paidBy, type, List.of(splits));
  }

  private static SplitRequest split(long memberId) {
    return new SplitRequest(memberId, null);
  }

  private static SplitRequest split(long memberId, String amount) {
    return new SplitRequest(memberId, new BigDecimal(amount));
  }

  private List<FieldErrorDto> errorsOf(CreateExpenseRequest request) {
    try {
      expenseService.createExpense(GROUP_ID, request);
    } catch (RequestValidationException ex) {
      verify(expenseRepository, never()).saveAndFlush(any());
      return ex.errors();
    }
    throw new AssertionError("expected RequestValidationException");
  }

  @Test
  void equalSplitDistributesRemainderDeterministically() {
    var response =
        expenseService.createExpense(
            GROUP_ID, request("100.00", 1, SplitType.EQUAL, split(3), split(1), split(2)));

    assertThat(response.amount()).isEqualByComparingTo("100.00");
    assertThat(response.paidBy().name()).isEqualTo("Alice");
    assertThat(response.splits())
        .extracting(SplitResponse::memberId, s -> s.amount().toPlainString())
        .containsExactly(
            tuple(1L, "33.34"),
            tuple(2L, "33.33"),
            tuple(3L, "33.33"));
  }

  @Test
  void exactSplitKeepsGivenShares() {
    var response =
        expenseService.createExpense(
            GROUP_ID, request("50.00", 2, SplitType.EXACT, split(1, "20.50"), split(2, "29.50")));

    assertThat(response.splits())
        .extracting(s -> s.amount().toPlainString())
        .containsExactly("20.50", "29.50");
  }

  @Test
  void payerDoesNotHaveToBeAParticipant() {
    var response =
        expenseService.createExpense(GROUP_ID, request("10.00", 1, SplitType.EQUAL, split(2)));

    assertThat(response.splits()).singleElement().extracting(SplitResponse::memberId).isEqualTo(2L);
  }

  @Test
  void rejectsExactSplitThatDoesNotAddUp() {
    var request = request("100.00", 1, SplitType.EXACT, split(1, "60.00"), split(2, "39.99"));
    assertThat(errorsOf(request))
        .containsExactly(
            new FieldErrorDto(
                "splits", "split amounts add up to 99.99 but the expense amount is 100.00"));
  }

  @Test
  void rejectsPayerAndParticipantsOutsideTheGroup() {
    assertThat(errorsOf(request("10.00", 99, SplitType.EQUAL, split(1), split(77))))
        .extracting(FieldErrorDto::field)
        .containsExactly("paidBy", "splits[1].memberId");
  }

  @Test
  void rejectsDuplicateParticipants() {
    assertThat(errorsOf(request("10.00", 1, SplitType.EQUAL, split(2), split(2))))
        .extracting(FieldErrorDto::field, FieldErrorDto::message)
        .containsExactly(tuple("splits[1].memberId", "member 2 is listed more than once"));
  }

  @Test
  void rejectsAmountsOnEqualSplitAndMissingAmountsOnExactSplit() {
    assertThat(errorsOf(request("10.00", 1, SplitType.EQUAL, split(1, "10.00"))))
        .extracting(FieldErrorDto::field)
        .containsExactly("splits[0].amount");
    assertThat(errorsOf(request("10.00", 1, SplitType.EXACT, split(1, "10.00"), split(2))))
        .extracting(FieldErrorDto::field)
        .containsExactly("splits[1].amount");
  }

  @Test
  void rejectsUnknownGroup() {
    when(groupRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> expenseService.createExpense(5L, request("1.00", 1, SplitType.EQUAL, split(1))))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Group 5 not found");
  }

  @Test
  void getExpenseFromAnotherGroupIsNotFound() {
    when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
    when(expenseRepository.findWithDetailsByIdAndGroupId(10L, GROUP_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> expenseService.getExpense(GROUP_ID, 10L))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Expense 10 not found");
  }
}
