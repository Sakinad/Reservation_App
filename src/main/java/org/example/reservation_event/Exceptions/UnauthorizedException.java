package org.example.reservation_event.Exceptions;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}