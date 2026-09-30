package com.edstem.expense.common;

/** A single invalid field in a 400 response, exposed as {@code errors[]} on the ProblemDetail. */
public record FieldErrorDto(String field, String message) {}
