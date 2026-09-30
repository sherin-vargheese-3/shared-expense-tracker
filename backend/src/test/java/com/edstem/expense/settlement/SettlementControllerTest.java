package com.edstem.expense.settlement;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SettlementController.class)
class SettlementControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private SettlementService settlementService;

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"toMemberId\":1,\"amount\":5}                   | fromMemberId | must not be null",
        "{\"fromMemberId\":2,\"amount\":5}                 | toMemberId   | must not be null",
        "{\"fromMemberId\":2,\"toMemberId\":1}             | amount       | must not be null",
        "{\"fromMemberId\":2,\"toMemberId\":1,\"amount\":0} | amount       | must be greater than 0",
        "{\"fromMemberId\":2,\"toMemberId\":1,\"amount\":1.001} | amount | must have at most 2 decimal places",
      })
  void rejectsInvalidRequests(String json, String field, String message) throws Exception {
    mockMvc
        .perform(
            post("/api/groups/1/settlements").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem(field)))
        .andExpect(jsonPath("$.errors[*].message").value(hasItem(message)));
    verifyNoInteractions(settlementService);
  }
}
