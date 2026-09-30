package com.edstem.expense.expense;

import com.edstem.expense.group.ExpenseGroup;
import com.edstem.expense.group.Member;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense")
public class Expense {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private ExpenseGroup group;

  @Column(nullable = false, length = 255)
  private String description;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paid_by", nullable = false)
  private Member paidBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "split_type", nullable = false, length = 10)
  private SplitType splitType;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExpenseSplit> splits = new ArrayList<>();

  protected Expense() {}

  public Expense(
      ExpenseGroup group,
      String description,
      long amountCents,
      Member paidBy,
      SplitType splitType) {
    this.group = group;
    this.description = description;
    this.amountCents = amountCents;
    this.paidBy = paidBy;
    this.splitType = splitType;
  }

  @PrePersist
  void onCreate() {
    // Postgres TIMESTAMPTZ stores microseconds; match it so responses are stable.
    createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public void addSplit(Member member, long shareCents) {
    splits.add(new ExpenseSplit(this, member, shareCents));
  }

  public Long getId() {
    return id;
  }

  public ExpenseGroup getGroup() {
    return group;
  }

  public String getDescription() {
    return description;
  }

  public long getAmountCents() {
    return amountCents;
  }

  public Member getPaidBy() {
    return paidBy;
  }

  public SplitType getSplitType() {
    return splitType;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<ExpenseSplit> getSplits() {
    return splits;
  }
}
