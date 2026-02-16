package com.hoang.crypto.controller;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.User;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.repository.PriceAggregateRepository;
import com.hoang.crypto.repository.TransactionRepository;
import com.hoang.crypto.repository.UserRepository;
import com.hoang.crypto.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PriceAggregateRepository priceRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Long testUserId;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        priceRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("ratelimit-testuser");
        user = userRepository.save(user);
        testUserId = user.getId();

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrency(Currency.USDT);
        wallet.setBalance(new BigDecimal("100000.00"));
        walletRepository.save(wallet);

        PriceAggregate price = new PriceAggregate();
        price.setPair(CryptoPair.BTCUSDT);
        price.setBid(new BigDecimal("50000.00"));
        price.setAsk(new BigDecimal("50001.00"));
        price.setTimestamp(LocalDateTime.now());
        priceRepository.save(price);
    }

    @Test
    public void testPriceEndpointRateLimit() throws Exception {
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/crypto/price/latest").param("pair", "BTCUSDT"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/crypto/price/latest").param("pair", "BTCUSDT"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-Rate-Limit-Retry-After-Seconds"));
    }

    @Test
    public void testTradeEndpointRateLimit() throws Exception {
        String jsonRequest = String
                .format("{\"userId\": %d, \"pair\": \"BTCUSDT\", \"type\": \"BUY\", \"amount\": 0.001}", testUserId);

        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/crypto/trade")
                    .contentType("application/json")
                    .content(jsonRequest))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/crypto/trade")
                .contentType("application/json")
                .content(jsonRequest))
                .andExpect(status().isTooManyRequests());
    }
}
