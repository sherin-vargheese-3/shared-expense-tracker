package com.edstem.expense.group;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.expense.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GroupApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private long createGroup(String json) throws Exception {
    String body =
        mockMvc
            .perform(post("/api/groups").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private void addMember(long groupId, String name, int expectedStatus) throws Exception {
    mockMvc
        .perform(
            post("/api/groups/{id}/members", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
        .andExpect(status().is(expectedStatus));
  }

  @Test
  void groupLifecyclePersistsMembers() throws Exception {
    long groupId = createGroup("{\"name\":\" Weekend trip \",\"members\":[\"Alice\",\"Bob\"]}");

    addMember(groupId, "Charlie", 201);

    mockMvc
        .perform(get("/api/groups/{id}", groupId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Weekend trip"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.members.length()").value(3))
        .andExpect(jsonPath("$.members[2].name").value("Charlie"));

    mockMvc
        .perform(get("/api/groups/{id}/members", groupId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].name").value(hasItem("Bob")));

    mockMvc
        .perform(get("/api/groups"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == %d)].memberCount".formatted(groupId)).value(3));
  }

  @Test
  void duplicateMemberNamesAreRejectedCaseInsensitively() throws Exception {
    long groupId = createGroup("{\"name\":\"Flat\",\"members\":[\"Dana\"]}");

    addMember(groupId, "dana", 409);
    addMember(groupId, " DANA ", 409);
  }

  @Test
  void sameMemberNameIsAllowedInDifferentGroups() throws Exception {
    createGroup("{\"name\":\"G1\",\"members\":[\"Eve\"]}");
    long second = createGroup("{\"name\":\"G2\"}");

    addMember(second, "Eve", 201);
  }

  @Test
  void duplicateNamesInCreateRequestReturn400() throws Exception {
    mockMvc
        .perform(
            post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Trip\",\"members\":[\"Finn\",\"finn\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("members[1]"));
  }

  @Test
  void unknownGroupReturns404() throws Exception {
    mockMvc.perform(get("/api/groups/{id}", 999_999)).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/groups/{id}/members", 999_999)).andExpect(status().isNotFound());
    addMember(999_999, "Ghost", 404);
  }
}
