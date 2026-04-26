package com.hoang.crypto.service;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.User;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.repository.PriceAggregateRepository;
import com.hoang.crypto.repository.UserRepository;
import com.hoang.crypto.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ConcurrencyTradingTest {
    @Autowired
    private TradingService tradingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PriceAggregateRepository priceAggregateRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PriceService priceService;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Create a test user
        User user = new User();
        user.setUsername("concurrency_user_" + System.currentTimeMillis());
        user.setEmail("concurrency_user_" + System.currentTimeMillis() + "@example.com");
        user.setPassword("password");
        user = userRepository.save(user);
        testUserId = user.getId();

        // Mock security context
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(user.getUsername());
        
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        SecurityContextHolder.setContext(securityContext);
        
        // Mock UserService to return the test user
        when(userService.checkAndGetUser()).thenReturn(user);

        // Mock PriceService to return fixed price
        PriceAggregate fixedPrice = new PriceAggregate();
        fixedPrice.setPair(CryptoPair.ETHUSDT);
        fixedPrice.setBid(new BigDecimal("2000"));
        fixedPrice.setAsk(new BigDecimal("2100"));
        fixedPrice.setTimestamp(LocalDateTime.now());
        when(priceService.getLatestPrice(CryptoPair.ETHUSDT)).thenReturn(fixedPrice);

        // Add initial balance
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrency(Currency.USDT);
        wallet.setBalance(new BigDecimal("100000"));
        walletRepository.save(wallet);

    }

    @Test
    void testConcurrentTrades() throws InterruptedException {
        int threadCount = 10;
        BigDecimal tradeAmount = new BigDecimal("1"); // 1 ETH each time
        BigDecimal ethPrice = new BigDecimal("2100"); // Buy price (Ask)
        BigDecimal totalCost = ethPrice.multiply(tradeAmount).multiply(new BigDecimal(threadCount));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    tradingService.executeTrade(CryptoPair.ETHUSDT, "BUY", tradeAmount);
                } catch (Exception e) {
                    System.err.println("Trade failed: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify final USDT balance
        Wallet usdtWallet = walletRepository.findByUserIdAndCurrency(testUserId, Currency.USDT).orElseThrow();
        BigDecimal expectedBalance = new BigDecimal("100000").subtract(totalCost);

        // Use compareTo for BigDecimal equality check
        assertEquals(0, expectedBalance.compareTo(usdtWallet.getBalance()),
                "Balance mismatch! Expected: " + expectedBalance + " but got: " + usdtWallet.getBalance());

        // Verify ETH balance
        Wallet ethWallet = walletRepository.findByUserIdAndCurrency(testUserId, Currency.ETH).orElseThrow();
        assertEquals(0, new BigDecimal(threadCount).compareTo(ethWallet.getBalance()));
    }
}
