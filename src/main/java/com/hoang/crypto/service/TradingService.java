package com.hoang.crypto.service;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.Transaction;
import com.hoang.crypto.entity.User;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.repository.TransactionRepository;
import com.hoang.crypto.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PriceService priceService;
    private final UserService userService;

    @Transactional
    public Transaction executeTrade(CryptoPair pair, String type, BigDecimal amount) {
        User user = userService.checkAndGetUser();
        Long userId = user.getId();
        PriceAggregate latestPrice = priceService.getLatestPrice(pair);
        if (latestPrice == null) {
            throw new RuntimeException("No price available for " + pair);
        }

        String baseCurrencyStr = pair.name().replace("USDT", ""); // ETH, BTC
        Currency baseCurrency = Currency.valueOf(baseCurrencyStr);
        Currency quoteCurrency = Currency.USDT;

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid trade amount");
        }

        BigDecimal price = "BUY".equalsIgnoreCase(type) ? latestPrice.getAsk() : latestPrice.getBid();

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid price detected: " + price);
        }
        BigDecimal totalCost = price.multiply(amount);

        if ("BUY".equalsIgnoreCase(type)) {
            Wallet usdtWallet = walletRepository.findByUserIdAndCurrencyWithLock(userId, quoteCurrency)
                    .orElseThrow(() -> new RuntimeException("Insufficient funds (Wallet not found)"));

            if (usdtWallet.getBalance().compareTo(totalCost) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            usdtWallet.setBalance(usdtWallet.getBalance().subtract(totalCost));
            walletRepository.save(usdtWallet);

            Wallet baseWallet = walletRepository.findByUserIdAndCurrencyWithLock(userId, baseCurrency)
                    .orElseGet(() -> {
                        Wallet w = new Wallet();
                        w.setUser(user);
                        w.setCurrency(baseCurrency);
                        w.setBalance(BigDecimal.ZERO);
                        return w;
                    });
            baseWallet.setBalance(baseWallet.getBalance().add(amount));
            walletRepository.save(baseWallet);

        } else if ("SELL".equalsIgnoreCase(type)) {
            Wallet baseWallet = walletRepository.findByUserIdAndCurrencyWithLock(userId, baseCurrency)
                    .orElseThrow(() -> new RuntimeException("Insufficient crypto balance"));

            if (baseWallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient crypto balance");
            }

            baseWallet.setBalance(baseWallet.getBalance().subtract(amount));
            walletRepository.save(baseWallet);

            Wallet usdtWallet = walletRepository.findByUserIdAndCurrencyWithLock(userId, quoteCurrency)
                    .orElseGet(() -> {
                        Wallet w = new Wallet();
                        w.setUser(user);
                        w.setCurrency(quoteCurrency);
                        w.setBalance(BigDecimal.ZERO);
                        return w;
                    });
            usdtWallet.setBalance(usdtWallet.getBalance().add(totalCost));
            walletRepository.save(usdtWallet);
        } else {
            throw new IllegalArgumentException("Invalid trade type: " + type);
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setPair(pair);
        transaction.setType(type.toUpperCase());
        transaction.setPrice(price);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    public List<Wallet> getWalletBalance() {
        User user = userService.checkAndGetUser();
        return walletRepository.findByUserId(user.getId());
    }

    public List<Transaction> getTransactionHistory() {
        User user = userService.checkAndGetUser();
        return transactionRepository.findByUserId(user.getId());
    }
}
