package com.edstem.expense.group;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {

  boolean existsByGroupIdAndNameIgnoreCaseAndRemovedAtIsNull(Long groupId, String name);

  /** Active members only: those who can pay, share expenses and settle. */
  @Query(
      "SELECT m FROM Member m WHERE m.group.id = :groupId AND m.removedAt IS NULL ORDER BY m.id")
  List<Member> findActiveByGroupId(Long groupId);

  /** All members including removed ones, needed to reconcile historical balances. */
  List<Member> findByGroupIdOrderById(Long groupId);

  Optional<Member> findByIdAndGroupIdAndRemovedAtIsNull(Long id, Long groupId);
}
