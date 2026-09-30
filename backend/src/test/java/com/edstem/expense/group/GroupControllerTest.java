package com.edstem.expense.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.expense.common.ConflictException;
import com.edstem.expense.common.NotFoundException;
import com.edstem.expense.group.dto.GroupResponse;
import com.edstem.expense.group.dto.MemberResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private GroupService groupService;

  private static MockHttpServletRequestBuilder postJson(String url, String body) {
    return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
  }

  @Test
  void createGroupReturns201WithLocation() throws Exception {
    when(groupService.createGroup(any()))
        .thenReturn(
            new GroupResponse(
                7L, "Trip", Instant.now(), List.of(new MemberResponse(1L, "Alice"))));

    mockMvc
        .perform(postJson("/api/groups", "{\"name\":\"Trip\",\"members\":[\"Alice\"]}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/groups/7"))
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.members[0].name").value("Alice"));
  }

  @Test
  void createGroupRejectsBlankName() throws Exception {
    mockMvc
        .perform(postJson("/api/groups", "{\"name\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    verifyNoInteractions(groupService);
  }

  @Test
  void createGroupRejectsTooLongName() throws Exception {
    String longName = "x".repeat(101);
    mockMvc
        .perform(postJson("/api/groups", "{\"name\":\"" + longName + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
  }

  @Test
  void createGroupRejectsBlankMemberName() throws Exception {
    mockMvc
        .perform(postJson("/api/groups", "{\"name\":\"Trip\",\"members\":[\"Alice\",\"\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("members[1]"));
  }

  @Test
  void malformedJsonReturns400() throws Exception {
    mockMvc
        .perform(postJson("/api/groups", "{\"name\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Malformed request"));
  }

  @Test
  void nonNumericGroupIdReturns400() throws Exception {
    mockMvc.perform(get("/api/groups/abc")).andExpect(status().isBadRequest());
  }

  @Test
  void unknownGroupReturns404ProblemDetail() throws Exception {
    when(groupService.getGroup(42L)).thenThrow(NotFoundException.of("Group", 42L));

    mockMvc
        .perform(get("/api/groups/42"))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Group 42 not found"));
  }

  @Test
  void addMemberReturns201() throws Exception {
    when(groupService.addMember(eq(1L), any())).thenReturn(new MemberResponse(3L, "Charlie"));

    mockMvc
        .perform(postJson("/api/groups/1/members", "{\"name\":\"Charlie\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.name").value("Charlie"));
  }

  @Test
  void addDuplicateMemberReturns409() throws Exception {
    when(groupService.addMember(eq(1L), any()))
        .thenThrow(new ConflictException("A member named 'Alice' already exists in this group"));

    mockMvc
        .perform(postJson("/api/groups/1/members", "{\"name\":\"Alice\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("A member named 'Alice' already exists in this group"));
  }

  @Test
  void addMemberRejectsMissingName() throws Exception {
    mockMvc
        .perform(postJson("/api/groups/1/members", "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
  }

  @Test
  void deleteGroupReturns204() throws Exception {
    mockMvc.perform(delete("/api/groups/1")).andExpect(status().isNoContent());
    verify(groupService).deleteGroup(1L);
  }

  @Test
  void removeMemberReturns204() throws Exception {
    mockMvc.perform(delete("/api/groups/1/members/2")).andExpect(status().isNoContent());
    verify(groupService).removeMember(1L, 2L);
  }

  @Test
  void removeMemberWithBalanceReturns409() throws Exception {
    doThrow(new ConflictException("Bob still owes 5.00. Settle up before removing them."))
        .when(groupService)
        .removeMember(1L, 2L);

    mockMvc
        .perform(delete("/api/groups/1/members/2"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Bob still owes 5.00. Settle up before removing them."));
  }
}
