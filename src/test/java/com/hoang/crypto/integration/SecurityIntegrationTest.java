package com.hoang.crypto.integration;

import com.hoang.crypto.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SecurityIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testContextLoads() {
        // Basic test to verify Spring context loads properly
        // This test ensures security configuration doesn't prevent application startup
        assert userRepository != null;
    }
}
