package com.hoang.crypto.controller;

import com.hoang.crypto.exception.GlobalExceptionHandler;
import com.hoang.crypto.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleRuntimeException_BadRequest() {
        // Arrange
        RuntimeException exception = new RuntimeException("Insufficient balance");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<?> response = exceptionHandler.handleRuntimeException(exception, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        MessageResponse body = (MessageResponse) response.getBody();
        assertNotNull(body);
        assertEquals("Insufficient balance", body.getMessage());
    }

    @Test
    void handleResourceNotFoundException_NotFound() {
        // Arrange
        RuntimeException exception = new RuntimeException("User not found");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<?> response = exceptionHandler.resourceNotFoundException(exception, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        MessageResponse body = (MessageResponse) response.getBody();
        assertNotNull(body);
        assertEquals("User not found", body.getMessage());
    }
}
