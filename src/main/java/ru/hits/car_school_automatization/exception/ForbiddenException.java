package ru.hits.car_school_automatization.exception;

/**
 * Исключение (HTTP 403)
 */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}
