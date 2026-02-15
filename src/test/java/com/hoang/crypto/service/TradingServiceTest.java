package com.hoang.crypto.service;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.Transaction;
import com.hoang.crypto.entity.User;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.repository.TransactionRepository;
import com.hoang.crypto.repository.UserRepository;
import com.hoang.crypto.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PriceService priceService;

    @InjectMocks
    private TradingService tradingService;

    private User testUser;
    private PriceAggregate ethPrice;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        ethPrice = new PriceAggregate();
        ethPrice.setPair(CryptoPair.ETHUSDT);
        ethPrice.setBid(new BigDecimal("2000"));
        ethPrice.setAsk(new BigDecimal("2100"));
    }

    @Test
    void executeTrade_SuccessfulBuy() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(priceService.getLatestPrice(CryptoPair.ETHUSDT)).thenReturn(ethPrice);

        Wallet usdtWallet = new Wallet();
        usdtWallet.setCurrency(Currency.USDT);
        usdtWallet.setBalance(new BigDecimal("5000"));
        when(walletRepository.findByUserIdAndCurrencyWithLock(1L, Currency.USDT)).thenReturn(Optional.of(usdtWallet));

        when(walletRepository.findByUserIdAndCurrencyWithLock(1L, Currency.ETH)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        BigDecimal amount = new BigDecimal("1");
        Transaction result = tradingService.executeTrade(1L, CryptoPair.ETHUSDT, "BUY", amount);

        // Assert
        assertNotNull(result);
        assertEquals("BUY", result.getType());
        assertEquals(0, new BigDecimal("2100").compareTo(result.getPrice()));
        assertEquals(0, new BigDecimal("2900").compareTo(usdtWallet.getBalance()));
        verify(walletRepository, times(2)).save(any(Wallet.class));
    }

    @Test
    void executeTrade_SuccessfulSell() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(priceService.getLatestPrice(CryptoPair.ETHUSDT)).thenReturn(ethPrice);

        Wallet ethWallet = new Wallet();
        ethWallet.setCurrency(Currency.ETH);
        ethWallet.setBalance(new BigDecimal("2"));
        when(walletRepository.findByUserIdAndCurrencyWithLock(1L, Currency.ETH)).thenReturn(Optional.of(ethWallet));

        Wallet usdtWallet = new Wallet();
        usdtWallet.setCurrency(Currency.USDT);
        usdtWallet.setBalance(new BigDecimal("100"));
        when(walletRepository.findByUserIdAndCurrencyWithLock(1L, Currency.USDT)).thenReturn(Optional.of(usdtWallet));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Transaction result = tradingService.executeTrade(1L, CryptoPair.ETHUSDT, "SELL", new BigDecimal("1"));

        // Assert
        assertNotNull(result);
        assertEquals("SELL", result.getType());
        assertEquals(0, ethPrice.getBid().compareTo(result.getPrice()));
        assertEquals(0, new BigDecimal("1").compareTo(ethWallet.getBalance()));
        assertEquals(0, new BigDecimal("2100").compareTo(usdtWallet.getBalance()));
        verify(walletRepository, times(2)).save(any(Wallet.class));
    }

    @Test
    void executeTrade_InsufficientFundsBuy() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(priceService.getLatestPrice(CryptoPair.ETHUSDT)).thenReturn(ethPrice);

        Wallet usdtWallet = new Wallet();
        usdtWallet.setCurrency(Currency.USDT);
        usdtWallet.setBalance(new BigDecimal("1000"));
        when(walletRepository.findByUserIdAndCurrencyWithLock(1L, Currency.USDT)).thenReturn(Optional.of(usdtWallet));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tradingService.executeTrade(1L, CryptoPair.ETHUSDT, "BUY", new BigDecimal("1")));
        assertEquals("Insufficient balance", exception.getMessage());
    }

    @Test
    void executeTrade_InvalidAmount() {
        // Arrange - Need these because they are called before amount validation
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(priceService.getLatestPrice(CryptoPair.ETHUSDT)).thenReturn(ethPrice);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tradingService.executeTrade(1L, CryptoPair.ETHUSDT, "BUY", null));
        assertEquals("Invalid trade amount", exception.getMessage());

        RuntimeException exception2 = assertThrows(RuntimeException.class,
                () -> tradingService.executeTrade(1L, CryptoPair.ETHUSDT, "BUY", new BigDecimal("-1")));
        assertEquals("Invalid trade amount", exception2.getMessage());
    }

    @Test
    void executeTrade_MissingPrice() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(priceService.getLatestPrice(CryptoPair.ETHUSDT)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tradingService.executeTrade(1L, CryptoPair.ETHUSDT, "BUY", new BigDecimal("1")));
        assertEquals("No price available for ETHUSDT", exception.getMessage());
    }

    @Test
    void executeTrade_UserNotFound() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tradingService.executeTrade(99L, CryptoPair.ETHUSDT, "BUY", new BigDecimal("1")));
        assertEquals("User not found", exception.getMessage());
    }
}
