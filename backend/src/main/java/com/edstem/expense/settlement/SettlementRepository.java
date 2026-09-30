package com.edstem.expense.settlement;

import com.edstem.expense.balance.MemberAmount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

  @Query(
      """
      SELECT s FROM Settlement s
      JOIN FETCH s.fromMember
      JOIN FETCH s.toMember
      WHERE s.group.id = :groupId
      ORDER BY s.createdAt DESC, s.id DESC
      """)
  List<Settlement> findAllWithMembersByGroupId(Long groupId);

  @Query(
      """
      SELECT s.fromMember.id AS memberId, SUM(s.amountCents) AS cents
      FROM Settlement s WHERE s.group.id = :groupId GROUP BY s.fromMember.id
      """)
  List<MemberAmount> sumSentByMember(Long groupId);

  @Query(
      """
      SELECT s.toMember.id AS memberId, SUM(s.amountCents) AS cents
      FROM Settlement s WHERE s.group.id = :groupId GROUP BY s.toMember.id
      """)
  List<MemberAmount> sumReceivedByMember(Long groupId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM Settlement s WHERE s.group.id = :groupId")
  int deleteAllByGroupId(Long groupId);

  boolean existsByFromMemberIdOrToMemberId(Long fromMemberId, Long toMemberId);
}
