package com.edstem.expense.common;

import java.util.List;

/**
 * Thrown for input errors that cannot be expressed with Bean Validation annotations (e.g.
 * cross-field or cross-element rules). Mapped to HTTP 400 with the same shape as annotation
 * failures.
 */
public class RequestValidationException extends RuntimeException {

  private final List<FieldErrorDto> errors;

  public RequestValidationException(String field, String message) {
    this(List.of(new FieldErrorDto(field, message)));
  }

  public RequestValidationException(List<FieldErrorDto> errors) {
    super(errors.isEmpty() ? "Invalid request" : errors.getFirst().message());
    this.errors = List.copyOf(errors);
  }

  public List<FieldErrorDto> errors() {
    return errors;
  }
}
