package com.edstem.expense.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String PROBLEM_SCHEMA = "Problem";
  private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

  @Bean
  OpenAPI expenseTrackerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Shared Expense Tracker API")
                .version("v1")
                .description(
                    """
                    Track shared expenses within a group and settle up.

                    **Amounts** are decimals in a single currency with at most two decimal \
                    places (e.g. `12.50`). Internally they are stored as integer cents, so \
                    balances always sum to exactly zero.

                    **Balances**: positive means the member is owed money; negative means \
                    they owe.

                    **Errors** use RFC 7807 problem details. Validation failures (400) carry \
                    an `errors` array of `{field, message}`, where `field` is a JSON path \
                    such as `splits[1].amount`.
                    """));
  }

  /**
   * Adds the error responses every endpoint can return, all described by the {@code Problem}
   * schema. Controllers only declare their success response and endpoint-specific errors.
   */
  @Bean
  OpenApiCustomizer problemResponsesCustomizer() {
    return openApi -> {
      // Registered here rather than on the OpenAPI bean: springdoc rebuilds components.
      if (openApi.getComponents() == null) {
        openApi.setComponents(new Components());
      }
      openApi
          .getComponents()
          .addSchemas("FieldError", fieldErrorSchema())
          .addSchemas(PROBLEM_SCHEMA, problemSchema());
      openApi
          .getPaths()
          .forEach(
              (path, item) ->
                  item.readOperationsMap()
                      .forEach((method, op) -> addProblemResponses(path, method, op)));
    };
  }

  private static void addProblemResponses(
      String path, PathItem.HttpMethod method, Operation operation) {
    ApiResponses responses = operation.getResponses();
    if (method == PathItem.HttpMethod.POST) {
      addIfAbsent(responses, "400", "Invalid input; see `errors` for per-field messages");
    } else if (path.contains("{")) {
      addIfAbsent(responses, "400", "Malformed path parameter");
    }
    if (path.contains("{groupId}")) {
      addIfAbsent(responses, "404", "Group not found");
    }
    addIfAbsent(responses, "500", "Unexpected server error");
    responses.forEach(
        (code, response) -> {
          if (code.startsWith("4") || code.startsWith("5")) {
            response.setContent(problemContent());
          }
        });
  }

  private static void addIfAbsent(ApiResponses responses, String code, String description) {
    if (!responses.containsKey(code)) {
      responses.addApiResponse(code, new ApiResponse().description(description));
    }
  }

  private static Content problemContent() {
    return new Content()
        .addMediaType(
            PROBLEM_MEDIA_TYPE,
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA)));
  }

  private static Schema<?> fieldErrorSchema() {
    return new ObjectSchema()
        .addProperty("field", new StringSchema().example("splits[1].amount"))
        .addProperty("message", new StringSchema().example("must be greater than 0"));
  }

  private static Schema<?> problemSchema() {
    return new ObjectSchema()
        .description("RFC 7807 problem details")
        .addProperty("type", new StringSchema().example("about:blank"))
        .addProperty("title", new StringSchema().example("Validation failed"))
        .addProperty("status", new IntegerSchema().example(400))
        .addProperty("detail", new StringSchema().example("One or more fields are invalid"))
        .addProperty("instance", new StringSchema().example("/api/groups/1/expenses"))
        .addProperty(
            "errors",
            new ArraySchema()
                .description("Present on validation failures only")
                .items(new Schema<>().$ref("#/components/schemas/FieldError")));
  }
}
