package com.edstem.expense.group;

import com.edstem.expense.group.dto.GroupSummaryResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

  @EntityGraph(attributePaths = "members")
  Optional<ExpenseGroup> findWithMembersById(Long id);

  @Query(
      """
      SELECT new com.edstem.expense.group.dto.GroupSummaryResponse(
          g.id, g.name, COUNT(m), g.createdAt)
      FROM ExpenseGroup g LEFT JOIN g.members m ON m.removedAt IS NULL
      GROUP BY g.id, g.name, g.createdAt
      ORDER BY g.createdAt DESC, g.id DESC
      """)
  List<GroupSummaryResponse> findAllSummaries();

  /**
   * Deletes the group; ON DELETE CASCADE removes its members. Expenses and settlements reference
   * members, so they must be deleted first (see {@code GroupService#deleteGroup}).
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM ExpenseGroup g WHERE g.id = :groupId")
  int deleteGroupById(Long groupId);
}
