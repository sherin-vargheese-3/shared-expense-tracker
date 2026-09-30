package com.edstem.expense.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.expense.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RemovalApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private long groupId;
  private long alice;
  private long bob;
  private long carol;

  @BeforeEach
  void createGroup() throws Exception {
    String body =
        postJson("/api/groups", "{\"name\":\"Trip\",\"members\":[\"Alice\",\"Bob\",\"Carol\"]}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    groupId = ((Number) JsonPath.read(body, "$.id")).longValue();
    List<Integer> ids = JsonPath.read(body, "$.members[*].id");
    alice = ids.get(0);
    bob = ids.get(1);
    carol = ids.get(2);
  }

  private ResultActions postJson(String url, String json) throws Exception {
    return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private void addExpense(long paidBy, long... participants) throws Exception {
    StringBuilder splits = new StringBuilder();
    for (long p : participants) {
      splits.append(splits.isEmpty() ? "" : ",").append("{\"memberId\":").append(p).append('}');
    }
    postJson(
            "/api/groups/" + groupId + "/expenses",
            """
            {"description":"Dinner","amount":20,"paidBy":%d,"splitType":"EQUAL","splits":[%s]}
            """
                .formatted(paidBy, splits))
        .andExpect(status().isCreated());
  }

  private void settle(long from, long to, String amount) throws Exception {
    postJson(
            "/api/groups/" + groupId + "/settlements",
            "{\"fromMemberId\":%d,\"toMemberId\":%d,\"amount\":%s}".formatted(from, to, amount))
        .andExpect(status().isCreated());
  }

  private ResultActions removeMember(long memberId) throws Exception {
    return mockMvc.perform(delete("/api/groups/{g}/members/{m}", groupId, memberId));
  }

  private Integer memberRows(long memberId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM member WHERE id = ?", Integer.class, memberId);
  }

  @Test
  void memberWithoutHistoryIsDeleted() throws Exception {
    removeMember(carol).andExpect(status().isNoContent());

    assertThat(memberRows(carol)).isZero();
    mockMvc
        .perform(get("/api/groups/{id}", groupId))
        .andExpect(jsonPath("$.members.length()").value(2));
  }

  @Test
  void memberWithOutstandingBalanceCannotBeRemoved() throws Exception {
    addExpense(alice, alice, bob); // Bob owes Alice 10.00

    removeMember(bob)
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value("Bob still owes 10.00. Settle up before removing them."));
    removeMember(alice)
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value("Alice is still owed 10.00. Settle up before removing them."));
  }

  @Test
  void settledMemberWithHistoryIsSoftRemovedAndHistoryIsKept() throws Exception {
    addExpense(alice, alice, bob);
    settle(bob, alice, "10.00");

    removeMember(bob).andExpect(status().isNoContent());

    // Still in the database, hidden from the group, balances and member count.
    assertThat(memberRows(bob)).isOne();
    mockMvc
        .perform(get("/api/groups/{id}/members", groupId))
        .andExpect(jsonPath("$[*].name").value(not(hasItem("Bob"))));
    mockMvc
        .perform(get("/api/groups/{id}/balances", groupId))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].balance").value(not(hasItem(10.0))));
    mockMvc
        .perform(get("/api/groups"))
        .andExpect(jsonPath("$[?(@.id == %d)].memberCount".formatted(groupId)).value(2));

    // Past expenses and payments still name them.
    mockMvc
        .perform(get("/api/groups/{id}/expenses", groupId))
        .andExpect(jsonPath("$[0].splits[*].memberName").value(hasItem("Bob")));
    mockMvc
        .perform(get("/api/groups/{id}/settlements", groupId))
        .andExpect(jsonPath("$[0].from.name").value("Bob"));

    // They can't be used in new activity, and removing again is a 404.
    postJson(
            "/api/groups/" + groupId + "/expenses",
            """
            {"description":"x","amount":5,"paidBy":%d,"splitType":"EQUAL","splits":[{"memberId":%d}]}
            """
                .formatted(bob, alice))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("paidBy"));
    removeMember(bob).andExpect(status().isNotFound());

    // The name is free again, as a new member.
    postJson("/api/groups/" + groupId + "/members", "{\"name\":\"bob\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.removed").value(false));
  }

  @Test
  void historyDistinguishesAFormerMemberFromANewOneWithTheSameName() throws Exception {
    addExpense(alice, alice, bob);
    settle(bob, alice, "10.00");
    removeMember(bob).andExpect(status().isNoContent());
    String body =
        postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Bob\"}")
            .andReturn()
            .getResponse()
            .getContentAsString();
    long newBob = ((Number) JsonPath.read(body, "$.id")).longValue();
    addExpense(alice, alice, newBob);

    String oldBobSplit = "$[1].splits[?(@.memberId == %d)]".formatted(bob);
    String newBobSplit = "$[0].splits[?(@.memberId == %d)]".formatted(newBob);
    mockMvc
        .perform(get("/api/groups/{id}/expenses", groupId))
        .andExpect(jsonPath(oldBobSplit + ".memberName").value("Bob"))
        .andExpect(jsonPath(oldBobSplit + ".memberRemoved").value(true))
        .andExpect(jsonPath(newBobSplit + ".memberName").value("Bob"))
        .andExpect(jsonPath(newBobSplit + ".memberRemoved").value(false))
        .andExpect(jsonPath("$[0].paidBy.removed").value(false));
    mockMvc
        .perform(get("/api/groups/{id}/settlements", groupId))
        .andExpect(jsonPath("$[0].from.id").value(bob))
        .andExpect(jsonPath("$[0].from.removed").value(true))
        .andExpect(jsonPath("$[0].to.removed").value(false));
    mockMvc
        .perform(get("/api/groups/{id}", groupId))
        .andExpect(jsonPath("$.members[*].removed").value(not(hasItem(true))));
  }

  @Test
  void memberOfAnotherGroupIsNotFound() throws Exception {
    String other =
        postJson("/api/groups", "{\"name\":\"Other\",\"members\":[\"Zed\"]}")
            .andReturn()
            .getResponse()
            .getContentAsString();
    long zed = ((Number) JsonPath.read(other, "$.members[0].id")).longValue();

    removeMember(zed).andExpect(status().isNotFound());
    assertThat(memberRows(zed)).isOne();
  }

  @Test
  void deletingAGroupRemovesEverythingInIt() throws Exception {
    addExpense(alice, alice, bob, carol);
    settle(bob, alice, "5.00");

    mockMvc.perform(delete("/api/groups/{id}", groupId)).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/groups/{id}", groupId)).andExpect(status().isNotFound());
    for (String table : List.of("member", "expense", "settlement")) {
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM " + table + " WHERE group_id = ?", Integer.class, groupId))
          .as(table)
          .isZero();
    }
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense_split WHERE member_id IN (?, ?, ?)",
                Integer.class,
                alice,
                bob,
                carol))
        .isZero();

    mockMvc.perform(delete("/api/groups/{id}", groupId)).andExpect(status().isNotFound());
  }
}
