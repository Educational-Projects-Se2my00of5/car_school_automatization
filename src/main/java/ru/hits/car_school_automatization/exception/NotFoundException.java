package ru.hits.car_school_automatization.exception;

/**
 * Исключение (HTTP 404)
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
