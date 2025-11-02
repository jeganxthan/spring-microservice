package com.example.demo.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String message;

    // Empty constructor (required by Spring)
    public AuthResponse() {}

    // ✅ Factory method for token response
    public static AuthResponse withToken(String token) {
        AuthResponse response = new AuthResponse();
        response.token = token;
        return response;
    }

    // ✅ Factory method for message response
    public static AuthResponse withMessage(String message) {
        AuthResponse response = new AuthResponse();
        response.message = message;
        return response;
    }

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}
