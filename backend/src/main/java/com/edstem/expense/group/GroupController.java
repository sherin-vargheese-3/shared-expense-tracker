package com.edstem.expense.group;

import com.edstem.expense.group.dto.AddMemberRequest;
import com.edstem.expense.group.dto.CreateGroupRequest;
import com.edstem.expense.group.dto.GroupResponse;
import com.edstem.expense.group.dto.GroupSummaryResponse;
import com.edstem.expense.group.dto.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Groups", description = "Create groups and manage their members")
public class GroupController {

  private final GroupService groupService;

  public GroupController(GroupService groupService) {
    this.groupService = groupService;
  }

  @PostMapping
  @Operation(summary = "Create a group, optionally with initial members")
  @ApiResponse(responseCode = "201", description = "Group created")
  public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
    GroupResponse created = groupService.createGroup(request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  @Operation(summary = "List all groups, newest first")
  public List<GroupSummaryResponse> listGroups() {
    return groupService.listGroups();
  }

  @GetMapping("/{groupId}")
  @Operation(summary = "Get a group with its members")
  public GroupResponse getGroup(@PathVariable Long groupId) {
    return groupService.getGroup(groupId);
  }

  @DeleteMapping("/{groupId}")
  @Operation(summary = "Delete a group with all its members, expenses and settlements")
  @ApiResponse(responseCode = "204", description = "Group deleted")
  public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
    groupService.deleteGroup(groupId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{groupId}/members")
  @Operation(summary = "List the members of a group")
  public List<MemberResponse> listMembers(@PathVariable Long groupId) {
    return groupService.listMembers(groupId);
  }

  @PostMapping("/{groupId}/members")
  @Operation(summary = "Add a member to a group")
  @ApiResponse(responseCode = "201", description = "Member added")
  @ApiResponse(responseCode = "409", description = "Member name already used in this group")
  public ResponseEntity<MemberResponse> addMember(
      @PathVariable Long groupId, @Valid @RequestBody AddMemberRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(groupService.addMember(groupId, request));
  }

  @DeleteMapping("/{groupId}/members/{memberId}")
  @Operation(
      summary = "Remove a member from a group",
      description =
          "Only members with a zero balance can be removed. Members who appear in expenses or"
              + " settlements stay in that history but no longer appear in the group.")
  @ApiResponse(responseCode = "204", description = "Member removed")
  @ApiResponse(responseCode = "404", description = "Group or member not found")
  @ApiResponse(responseCode = "409", description = "Member still owes or is owed money")
  public ResponseEntity<Void> removeMember(
      @PathVariable Long groupId, @PathVariable Long memberId) {
    groupService.removeMember(groupId, memberId);
    return ResponseEntity.noContent().build();
  }
}
