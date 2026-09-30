package com.edstem.expense.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.expense.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BalanceApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

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
    return mockMvc.perform(
        post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  private void addEqualExpense(String amount, long paidBy, long... participants) throws Exception {
    StringBuilder splits = new StringBuilder();
    for (long p : participants) {
      splits.append(splits.isEmpty() ? "" : ",").append("{\"memberId\":").append(p).append('}');
    }
    postJson(
            "/api/groups/" + groupId + "/expenses",
            """
            {"description":"e","amount":%s,"paidBy":%d,"splitType":"EQUAL","splits":[%s]}
            """
                .formatted(amount, paidBy, splits))
        .andExpect(status().isCreated());
  }

  private void settle(long from, long to, String amount) throws Exception {
    postJson(
            "/api/groups/" + groupId + "/settlements",
            "{\"fromMemberId\":%d,\"toMemberId\":%d,\"amount\":%s}".formatted(from, to, amount))
        .andExpect(status().isCreated());
  }

  private List<Map<String, Object>> getJson(String path) throws Exception {
    String body =
        mockMvc
            .perform(get("/api/groups/{id}/" + path, groupId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$");
  }

  private static BigDecimal sumOf(List<Map<String, Object>> rows, String field) {
    return rows.stream()
        .map(r -> new BigDecimal(r.get(field).toString()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Test
  void newGroupHasZeroBalancesAndNothingToSettle() throws Exception {
    mockMvc
        .perform(get("/api/groups/{id}/balances", groupId))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].memberName").value("Alice"))
        .andExpect(jsonPath("$[0].balance").value(0.0));
    assertThat(getJson("suggested-payments")).isEmpty();
  }

  @Test
  void balancesReflectExpensesAndSumToZeroDespiteRounding() throws Exception {
    addEqualExpense("100.00", alice, alice, bob, carol); // 33.34 / 33.33 / 33.33
    addEqualExpense("10.00", bob, alice, bob, carol); // 3.34 / 3.33 / 3.33

    mockMvc
        .perform(get("/api/groups/{id}/balances", groupId))
        .andExpect(jsonPath("$[0].balance").value(63.32)) // 100 - 33.34 - 3.34
        .andExpect(jsonPath("$[1].balance").value(-26.66)) // 10 - 33.33 - 3.33
        .andExpect(jsonPath("$[2].balance").value(-36.66)); // -33.33 - 3.33

    assertThat(sumOf(getJson("balances"), "balance")).isEqualByComparingTo("0");
  }

  @Test
  void recordingSuggestedPaymentsSettlesEveryone() throws Exception {
    addEqualExpense("90.00", alice, alice, bob, carol);
    addEqualExpense("45.50", carol, bob, carol);

    List<Map<String, Object>> suggestions = getJson("suggested-payments");
    assertThat(suggestions).isNotEmpty().hasSizeLessThanOrEqualTo(2);

    for (Map<String, Object> s : suggestions) {
      settle(
          ((Number) s.get("fromMemberId")).longValue(),
          ((Number) s.get("toMemberId")).longValue(),
          s.get("amount").toString());
    }

    assertThat(getJson("balances"))
        .allSatisfy(b -> assertThat(new BigDecimal(b.get("balance").toString())).isZero());
    assertThat(getJson("suggested-payments")).isEmpty();
    assertThat(getJson("settlements")).hasSize(suggestions.size());
  }

  @Test
  void partialAndOverpaymentsAdjustBalances() throws Exception {
    addEqualExpense("60.00", alice, alice, bob); // Bob owes Alice 30.00

    settle(bob, alice, "10.00");
    mockMvc
        .perform(get("/api/groups/{id}/balances", groupId))
        .andExpect(jsonPath("$[0].balance").value(20.0))
        .andExpect(jsonPath("$[1].balance").value(-20.0));

    settle(bob, alice, "25.00"); // overpays by 5.00, so Alice now owes Bob
    mockMvc
        .perform(get("/api/groups/{id}/suggested-payments", groupId))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].fromMemberName").value("Alice"))
        .andExpect(jsonPath("$[0].toMemberName").value("Bob"))
        .andExpect(jsonPath("$[0].amount").value(5.0));
  }

  @Test
  void settlementValidationUsesTheDatabase() throws Exception {
    postJson(
            "/api/groups/" + groupId + "/settlements",
            "{\"fromMemberId\":%d,\"toMemberId\":%d,\"amount\":5}".formatted(alice, alice))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("toMemberId"));
    postJson(
            "/api/groups/" + groupId + "/settlements",
            "{\"fromMemberId\":%d,\"toMemberId\":999999,\"amount\":5}".formatted(alice))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("toMemberId"));
    assertThat(getJson("settlements")).isEmpty();
  }

  @Test
  void unknownGroupIsNotFound() throws Exception {
    mockMvc.perform(get("/api/groups/999999/balances")).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/groups/999999/suggested-payments")).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/groups/999999/settlements")).andExpect(status().isNotFound());
    postJson("/api/groups/999999/settlements", "{\"fromMemberId\":1,\"toMemberId\":2,\"amount\":1}")
        .andExpect(status().isNotFound());
  }
}
