package com.edstem.expense;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ApplicationIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void healthEndpointReportsUp() {
    var response = restTemplate.getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }

  @Test
  void flywayCreatesSchema() {
    List<String> tables =
        jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String.class);

    assertThat(tables)
        .contains("expense_group", "member", "expense", "expense_split", "settlement");
  }

  @Test
  void openApiDocsAreServed() {
    var response = restTemplate.getForEntity("/v3/api-docs", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Shared Expense Tracker API");
  }

  @Test
  void openApiDocumentsSuccessAndProblemResponses() {
    DocumentContext spec =
        JsonPath.parse(restTemplate.getForObject("/v3/api-docs", String.class));
    String balances = "$.paths['/api/groups/{groupId}/balances'].get.responses";
    String createExpense = "$.paths['/api/groups/{groupId}/expenses'].post.responses";

    // Declaring error responses must not hide the generated success response.
    assertThat(spec.read(balances + "['200'].content", Map.class)).containsKey("application/json");
    assertThat(spec.read(createExpense + "['201'].description", String.class))
        .isEqualTo("Expense recorded");

    String notFoundSchema = balances + "['404'].content['application/problem+json'].schema.$ref";
    assertThat(spec.read(notFoundSchema, String.class))
        .isEqualTo("#/components/schemas/Problem");
    assertThat(spec.read(createExpense + "['400'].content", Map.class))
        .containsKey("application/problem+json");
    assertThat(spec.read("$.components.schemas.Problem.properties", Map.class))
        .containsKeys("status", "detail", "errors");
  }
}
