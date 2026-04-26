package com.hoang.crypto.service;

import com.hoang.crypto.entity.User;
import com.hoang.crypto.exception.ResourceNotFoundException;
import com.hoang.crypto.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword("password");
    }

    @Test
    void checkAndGetUser_Success() {
        // Arrange
        String username = "testuser";
        when(userDetails.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.checkAndGetUser();

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void checkAndGetUser_UserNotFound() {
        // Arrange
        String username = "nonexistentuser";
        when(userDetails.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.checkAndGetUser());
        assertEquals("User is empty", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void checkAndGetUser_NullAuthentication() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> userService.checkAndGetUser());
    }

    @Test
    void checkAndGetUser_NullPrincipal() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> userService.checkAndGetUser());
    }
}
