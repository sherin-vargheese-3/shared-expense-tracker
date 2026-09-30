package com.edstem.expense.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ExpenseApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private long groupId;
  private long alice;
  private long bob;
  private long carol;

  @BeforeEach
  void createGroup() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Trip\",\"members\":[\"Alice\",\"Bob\",\"Carol\"]}"))
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

  private ResultActions postExpense(long group, String json) throws Exception {
    return mockMvc.perform(
        post("/api/groups/{id}/expenses", group)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  @Test
  void equalExpenseIsPersistedWithExactShares() throws Exception {
    String json =
        """
        {"description":" Dinner ","amount":100,"paidBy":%d,"splitType":"EQUAL",
         "splits":[{"memberId":%d},{"memberId":%d},{"memberId":%d}]}
        """
            .formatted(alice, carol, bob, alice);
    String body =
        postExpense(groupId, json)
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.description").value("Dinner"))
            .andExpect(jsonPath("$.amount").value(100.00))
            .andExpect(jsonPath("$.paidBy.name").value("Alice"))
            .andExpect(jsonPath("$.splits[0].memberName").value("Alice"))
            .andExpect(jsonPath("$.splits[0].amount").value(33.34))
            .andExpect(jsonPath("$.splits[1].amount").value(33.33))
            .andExpect(jsonPath("$.splits[2].amount").value(33.33))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long expenseId = ((Number) JsonPath.read(body, "$.id")).longValue();

    Long storedTotal =
        jdbcTemplate.queryForObject(
            "SELECT SUM(share_cents) FROM expense_split WHERE expense_id = ?",
            Long.class,
            expenseId);
    assertThat(storedTotal).isEqualTo(10000L);

    mockMvc
        .perform(get("/api/groups/{g}/expenses/{e}", groupId, expenseId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.splitType").value("EQUAL"))
        .andExpect(jsonPath("$.splits.length()").value(3));
  }

  @Test
  void exactExpenseAndListingNewestFirst() throws Exception {
    postExpense(
            groupId,
            """
            {"description":"Taxi","amount":"30.00","paidBy":%d,"splitType":"EQUAL",
             "splits":[{"memberId":%d},{"memberId":%d}]}
            """
                .formatted(bob, alice, bob))
        .andExpect(status().isCreated());
    postExpense(
            groupId,
            """
            {"description":"Hotel","amount":250.50,"paidBy":%d,"splitType":"EXACT",
             "splits":[{"memberId":%d,"amount":100.25},{"memberId":%d,"amount":150.25}]}
            """
                .formatted(carol, alice, carol))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.splits[1].amount").value(150.25));

    mockMvc
        .perform(get("/api/groups/{id}/expenses", groupId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].description").value("Hotel"))
        .andExpect(jsonPath("$[0].splits.length()").value(2))
        .andExpect(jsonPath("$[1].description").value("Taxi"));
  }

  @Test
  void memberFromAnotherGroupCannotBeUsed() throws Exception {
    long outsider =
        ((Number)
                JsonPath.read(
                    mockMvc
                        .perform(
                            post("/api/groups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Other\",\"members\":[\"Zed\"]}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                    "$.members[0].id"))
            .longValue();

    postExpense(
            groupId,
            """
            {"description":"x","amount":10,"paidBy":%d,"splitType":"EQUAL",
             "splits":[{"memberId":%d}]}
            """
                .formatted(outsider, alice))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("paidBy"));
  }

  @Test
  void mismatchedExactSplitIsRejectedAndNothingIsStored() throws Exception {
    postExpense(
            groupId,
            """
            {"description":"x","amount":10,"paidBy":%d,"splitType":"EXACT",
             "splits":[{"memberId":%d,"amount":4},{"memberId":%d,"amount":5}]}
            """
                .formatted(alice, alice, bob))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("split amounts add up to 9.00 but the expense amount is 10.00"));

    mockMvc
        .perform(get("/api/groups/{id}/expenses", groupId))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void unknownGroupOrExpenseIsNotFound() throws Exception {
    postExpense(
            999_999,
            """
            {"description":"x","amount":1,"paidBy":1,"splitType":"EQUAL","splits":[{"memberId":1}]}
            """)
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/api/groups/{id}/expenses", 999_999)).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/groups/{g}/expenses/{e}", groupId, 999_999))
        .andExpect(status().isNotFound());
  }
}
