package com.ecommerce.auth.exception;

public class AuthUserAlreadyExistsException extends RuntimeException {

    public AuthUserAlreadyExistsException(String fieldName, String fieldValue) {
        super("Auth user already exists with " + fieldName + " " + fieldValue);
    }
}
