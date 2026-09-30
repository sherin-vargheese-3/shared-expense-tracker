package com.edstem.expense.common;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates exceptions into RFC 7807 {@link ProblemDetail} responses. Validation failures carry
 * an {@code errors} array of {@code {field, message}} objects.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String ERRORS = "errors";

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldErrorDto> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new FieldErrorDto(e.getField(), e.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest().body(validationProblem(errors));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    // Well-formed JSON with a wrongly typed field (e.g. "amount": "abc", "splitType": "HALF")
    // is reported against that field, like any other validation error.
    if (ex.getCause() instanceof MismatchedInputException mismatch
        && !mismatch.getPath().isEmpty()) {
      FieldErrorDto error = new FieldErrorDto(jsonPath(mismatch), typeMessage(mismatch));
      return ResponseEntity.badRequest().body(validationProblem(List.of(error)));
    }
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON");
    problem.setTitle("Malformed request");
    return ResponseEntity.badRequest().body(problem);
  }

  private static String jsonPath(MismatchedInputException ex) {
    StringBuilder path = new StringBuilder();
    for (JsonMappingException.Reference ref : ex.getPath()) {
      if (ref.getFieldName() != null) {
        if (!path.isEmpty()) {
          path.append('.');
        }
        path.append(ref.getFieldName());
      } else {
        path.append('[').append(ref.getIndex()).append(']');
      }
    }
    return path.toString();
  }

  private static String typeMessage(MismatchedInputException ex) {
    Class<?> target = ex.getTargetType();
    if (target != null && target.isEnum()) {
      return "must be one of " + Arrays.toString(target.getEnumConstants());
    }
    if (target != null && Number.class.isAssignableFrom(target)) {
      return "must be a number";
    }
    return "has an invalid value";
  }

  @ExceptionHandler(RequestValidationException.class)
  ProblemDetail handleRequestValidation(RequestValidationException ex) {
    return validationProblem(ex.errors());
  }

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Not found");
    return problem;
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    return problem;
  }

  /** Last line of defence for constraint violations that slipped past service-level checks. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "The request conflicts with existing data");
    problem.setTitle("Conflict");
    return problem;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setTitle("Internal server error");
    return problem;
  }

  private static ProblemDetail validationProblem(List<FieldErrorDto> errors) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid");
    problem.setTitle("Validation failed");
    problem.setProperty(ERRORS, errors);
    return problem;
  }
}
