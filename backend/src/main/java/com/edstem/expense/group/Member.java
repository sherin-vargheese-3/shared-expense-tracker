package com.edstem.expense.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "member")
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private ExpenseGroup group;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Set when a member with history leaves the group; null while active. */
  @Column(name = "removed_at")
  private Instant removedAt;

  protected Member() {}

  Member(ExpenseGroup group, String name) {
    this.group = group;
    this.name = name;
  }

  @PrePersist
  void onCreate() {
    // Postgres TIMESTAMPTZ stores microseconds; match it so responses are stable.
    createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  /** Soft-removes the member: hidden from the group, but kept for expense/settlement history. */
  public void markRemoved() {
    removedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public boolean isActive() {
    return removedAt == null;
  }

  public Long getId() {
    return id;
  }

  public ExpenseGroup getGroup() {
    return group;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
