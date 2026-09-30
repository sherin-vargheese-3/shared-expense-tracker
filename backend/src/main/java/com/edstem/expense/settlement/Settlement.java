package com.edstem.expense.settlement;

import com.edstem.expense.group.ExpenseGroup;
import com.edstem.expense.group.Member;
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

/** A payment from one member to another that pays down what they owe. */
@Entity
@Table(name = "settlement")
public class Settlement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private ExpenseGroup group;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "from_member", nullable = false)
  private Member fromMember;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "to_member", nullable = false)
  private Member toMember;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Settlement() {}

  public Settlement(ExpenseGroup group, Member fromMember, Member toMember, long amountCents) {
    this.group = group;
    this.fromMember = fromMember;
    this.toMember = toMember;
    this.amountCents = amountCents;
  }

  @PrePersist
  void onCreate() {
    // Postgres TIMESTAMPTZ stores microseconds; match it so responses are stable.
    createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public Long getId() {
    return id;
  }

  public ExpenseGroup getGroup() {
    return group;
  }

  public Member getFromMember() {
    return fromMember;
  }

  public Member getToMember() {
    return toMember;
  }

  public long getAmountCents() {
    return amountCents;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
