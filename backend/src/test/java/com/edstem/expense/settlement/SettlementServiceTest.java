package com.edstem.expense.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.expense.common.FieldErrorDto;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.common.RequestValidationException;
import com.edstem.expense.group.ExpenseGroup;
import com.edstem.expense.group.ExpenseGroupRepository;
import com.edstem.expense.group.Member;
import com.edstem.expense.group.MemberRepository;
import com.edstem.expense.settlement.dto.CreateSettlementRequest;
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
class SettlementServiceTest {

  @Mock private SettlementRepository settlementRepository;
  @Mock private ExpenseGroupRepository groupRepository;
  @Mock private MemberRepository memberRepository;
  @InjectMocks private SettlementService settlementService;

  @BeforeEach
  void setUp() {
    ExpenseGroup group = new ExpenseGroup("Trip");
    Member alice = group.addMember("Alice");
    Member bob = group.addMember("Bob");
    ReflectionTestUtils.setField(alice, "id", 1L);
    ReflectionTestUtils.setField(bob, "id", 2L);
    when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
    when(memberRepository.findActiveByGroupId(1L)).thenReturn(List.of(alice, bob));
    when(settlementRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private static CreateSettlementRequest request(long from, long to, String amount) {
    return new CreateSettlementRequest(from, to, new BigDecimal(amount));
  }

  @Test
  void recordsSettlementInCents() {
    var response = settlementService.recordSettlement(1L, request(2, 1, "12.34"));

    assertThat(response.from().name()).isEqualTo("Bob");
    assertThat(response.to().name()).isEqualTo("Alice");
    assertThat(response.amount()).isEqualByComparingTo("12.34");
  }

  @Test
  void rejectsPayingYourself() {
    assertThatThrownBy(() -> settlementService.recordSettlement(1L, request(1, 1, "5")))
        .isInstanceOfSatisfying(
            RequestValidationException.class,
            ex ->
                assertThat(ex.errors())
                    .containsExactly(
                        new FieldErrorDto("toMemberId", "must be different from fromMemberId")));
    verify(settlementRepository, never()).saveAndFlush(any());
  }

  @Test
  void rejectsMembersOutsideTheGroup() {
    assertThatThrownBy(() -> settlementService.recordSettlement(1L, request(8, 9, "5")))
        .isInstanceOfSatisfying(
            RequestValidationException.class,
            ex ->
                assertThat(ex.errors())
                    .extracting(FieldErrorDto::field)
                    .containsExactly("fromMemberId", "toMemberId"));
  }

  @Test
  void rejectsUnknownGroup() {
    when(groupRepository.findById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> settlementService.recordSettlement(7L, request(1, 2, "5")))
        .isInstanceOf(NotFoundException.class);
  }
}
