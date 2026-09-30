package com.edstem.expense.common;

/** Thrown when a requested resource does not exist. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }

  public static NotFoundException of(String resource, Object id) {
    return new NotFoundException("%s %s not found".formatted(resource, id));
  }
}
