package com.sportrental.equipment.error;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
