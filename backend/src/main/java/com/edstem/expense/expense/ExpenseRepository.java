package com.edstem.expense.expense;

import com.edstem.expense.balance.MemberAmount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

  /** Loads a group's expenses with payer and splits in a single query (no N+1). */
  @Query(
      """
      SELECT DISTINCT e FROM Expense e
      JOIN FETCH e.paidBy
      LEFT JOIN FETCH e.splits s
      LEFT JOIN FETCH s.member
      WHERE e.group.id = :groupId
      ORDER BY e.createdAt DESC, e.id DESC
      """)
  List<Expense> findAllWithDetailsByGroupId(Long groupId);

  @Query(
      """
      SELECT e FROM Expense e
      JOIN FETCH e.paidBy
      LEFT JOIN FETCH e.splits s
      LEFT JOIN FETCH s.member
      WHERE e.id = :expenseId AND e.group.id = :groupId
      """)
  Optional<Expense> findWithDetailsByIdAndGroupId(Long expenseId, Long groupId);

  @Query(
      """
      SELECT e.paidBy.id AS memberId, SUM(e.amountCents) AS cents
      FROM Expense e WHERE e.group.id = :groupId GROUP BY e.paidBy.id
      """)
  List<MemberAmount> sumPaidByMember(Long groupId);

  @Query(
      """
      SELECT s.member.id AS memberId, SUM(s.shareCents) AS cents
      FROM ExpenseSplit s WHERE s.expense.group.id = :groupId GROUP BY s.member.id
      """)
  List<MemberAmount> sumOwedByMember(Long groupId);

  /** Bulk-deletes a group's expenses; ON DELETE CASCADE removes their splits. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM Expense e WHERE e.group.id = :groupId")
  int deleteAllByGroupId(Long groupId);

  /** Whether the member paid for, or shared in, any expense. */
  @Query(
      """
      SELECT COUNT(e) > 0 FROM Expense e
      WHERE e.paidBy.id = :memberId
         OR EXISTS (SELECT 1 FROM ExpenseSplit s WHERE s.expense = e AND s.member.id = :memberId)
      """)
  boolean isMemberInvolved(Long memberId);
}
