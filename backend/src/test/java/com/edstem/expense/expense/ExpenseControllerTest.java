package com.edstem.expense.expense;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ExpenseService expenseService;

  private ResultActions postExpense(String json) throws Exception {
    return mockMvc.perform(
        post("/api/groups/1/expenses").contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private static String body(String amount) {
    return """
        {"description":"Dinner","amount":%s,"paidBy":1,"splitType":"EQUAL",
         "splits":[{"memberId":1},{"memberId":2}]}
        """
        .formatted(amount);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "0       | must be greater than 0",
        "-5.00   | must be greater than 0",
        "10.005  | must have at most 2 decimal places",
        "null    | must not be null",
      })
  void rejectsInvalidAmounts(String amount, String message) throws Exception {
    postExpense(body(amount))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("amount")))
        .andExpect(jsonPath("$.errors[*].message").value(hasItem(message)));
    verifyNoInteractions(expenseService);
  }

  @Test
  void rejectsNonNumericAmountAgainstTheField() throws Exception {
    postExpense(body("\"ten\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("amount"))
        .andExpect(jsonPath("$.errors[0].message").value("must be a number"));
  }

  @Test
  void rejectsUnknownSplitType() throws Exception {
    postExpense(
            """
            {"description":"x","amount":1,"paidBy":1,"splitType":"HALF","splits":[{"memberId":1}]}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("splitType"))
        .andExpect(jsonPath("$.errors[0].message").value(containsString("EQUAL, EXACT")));
  }

  @Test
  void reportsNestedFieldPathForBadSplitMemberId() throws Exception {
    postExpense(
            """
            {"description":"x","amount":1,"paidBy":1,"splitType":"EQUAL",
             "splits":[{"memberId":1},{"memberId":"bob"}]}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("splits[1].memberId"));
  }

  @Test
  void rejectsMissingFieldsAndEmptySplits() throws Exception {
    postExpense("{\"amount\":1,\"splits\":[]}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(4))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("description")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("paidBy")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("splitType")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("splits")));
  }

  @Test
  void rejectsInvalidExactSplitAmount() throws Exception {
    postExpense(
            """
            {"description":"x","amount":1,"paidBy":1,"splitType":"EXACT",
             "splits":[{"memberId":1,"amount":0.999}]}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("splits[0].amount"));
  }
}
