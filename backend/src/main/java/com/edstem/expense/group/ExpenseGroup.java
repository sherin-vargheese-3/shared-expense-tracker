package com.edstem.expense.group;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense_group")
public class ExpenseGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private List<Member> members = new ArrayList<>();

  protected ExpenseGroup() {}

  public ExpenseGroup(String name) {
    this.name = name;
  }

  @PrePersist
  void onCreate() {
    // Postgres TIMESTAMPTZ stores microseconds; match it so responses are stable.
    createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public Member addMember(String memberName) {
    Member member = new Member(this, memberName);
    members.add(member);
    return member;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<Member> getMembers() {
    return members;
  }
}
