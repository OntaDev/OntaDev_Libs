package com.ontadev.libs.exception;

public class UnsupportedDatabaseFeatureException extends RuntimeException {

    public UnsupportedDatabaseFeatureException(String message) {
        super(message);
    }
}