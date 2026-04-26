package com.hoang.crypto.service;

import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.User;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TradingService tradingService; // Using TradingService as it contains wallet operations

    private User testUser;
    private Wallet usdtWallet;
    private Wallet ethWallet;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);

        usdtWallet = new Wallet();
        usdtWallet.setId(UUID.randomUUID());
        usdtWallet.setUser(testUser);
        usdtWallet.setCurrency(Currency.USDT);
        usdtWallet.setBalance(new BigDecimal("10000.00"));

        ethWallet = new Wallet();
        ethWallet.setId(UUID.randomUUID());
        ethWallet.setUser(testUser);
        ethWallet.setCurrency(Currency.ETH);
        ethWallet.setBalance(new BigDecimal("2.5"));
    }

    @Test
    void testGetWalletBalance_Success() {
        // Arrange
        when(userService.checkAndGetUser()).thenReturn(testUser);
        when(walletRepository.findByUserId(testUser.getId()))
                .thenReturn(Arrays.asList(usdtWallet, ethWallet));

        // Act
        List<Wallet> result = tradingService.getWalletBalance();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(usdtWallet));
        assertTrue(result.contains(ethWallet));
        verify(walletRepository, times(1)).findByUserId(testUser.getId());
    }

    @Test
    void testGetWalletBalance_EmptyList() {
        // Arrange
        when(userService.checkAndGetUser()).thenReturn(testUser);
        when(walletRepository.findByUserId(testUser.getId()))
                .thenReturn(Arrays.asList());

        // Act
        List<Wallet> result = tradingService.getWalletBalance();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(walletRepository, times(1)).findByUserId(testUser.getId());
    }

    @Test
    void testWalletCreation_NewWallet() {
        // Arrange
        Wallet newWallet = new Wallet();
        newWallet.setUser(testUser);
        newWallet.setCurrency(Currency.BTC);
        newWallet.setBalance(BigDecimal.ZERO);
        newWallet.setId(UUID.randomUUID());

        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        // Act
        Wallet savedWallet = walletRepository.save(newWallet);

        // Assert
        assertNotNull(savedWallet);
        assertEquals(Currency.BTC, savedWallet.getCurrency());
        assertEquals(BigDecimal.ZERO, savedWallet.getBalance());
        assertEquals(testUser, savedWallet.getUser());
    }

    @Test
    void testWalletBalanceUpdate() {
        // Arrange
        BigDecimal newBalance = new BigDecimal("15000.00");
        usdtWallet.setBalance(newBalance);

        when(walletRepository.save(usdtWallet)).thenReturn(usdtWallet);

        // Act
        Wallet updatedWallet = walletRepository.save(usdtWallet);

        // Assert
        assertNotNull(updatedWallet);
        assertEquals(0, newBalance.compareTo(updatedWallet.getBalance()));
        verify(walletRepository, times(1)).save(usdtWallet);
    }

    @Test
    void testFindByUserIdAndCurrency_Success() {
        // Arrange
        when(walletRepository.findByUserIdAndCurrencyWithLock(testUser.getId(), Currency.USDT))
                .thenReturn(Optional.of(usdtWallet));

        // Act
        Optional<Wallet> result = walletRepository.findByUserIdAndCurrencyWithLock(testUser.getId(), Currency.USDT);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(usdtWallet, result.get());
        assertEquals(Currency.USDT, result.get().getCurrency());
        verify(walletRepository, times(1)).findByUserIdAndCurrencyWithLock(testUser.getId(), Currency.USDT);
    }

    @Test
    void testFindByUserIdAndCurrency_NotFound() {
        // Arrange
        when(walletRepository.findByUserIdAndCurrencyWithLock(testUser.getId(), Currency.BTC))
                .thenReturn(Optional.empty());

        // Act
        Optional<Wallet> result = walletRepository.findByUserIdAndCurrencyWithLock(testUser.getId(), Currency.BTC);

        // Assert
        assertFalse(result.isPresent());
        verify(walletRepository, times(1)).findByUserIdAndCurrencyWithLock(testUser.getId(), Currency.BTC);
    }
}
