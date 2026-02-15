package com.hoang.crypto.service;

import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.Transaction;
import com.hoang.crypto.entity.User;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.repository.TransactionRepository;
import com.hoang.crypto.repository.UserRepository;
import com.hoang.crypto.repository.WalletRepository;
import jakarta.annotation.PostConstruct;
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

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PriceService priceService;

    @PostConstruct
    public void init() {
        // Seed data
        if (userRepository.count() == 0) {
            User user = new User();
            user.setUsername("testuser");
            userRepository.save(user);

            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setCurrency("USDT");
            wallet.setBalance(new BigDecimal("50000"));
            walletRepository.save(wallet);
            log.info("Seeded user with 50,000 USDT");
        }
    }

    @Transactional
    public Transaction executeTrade(Long userId, String pair, String type, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        PriceAggregate latestPrice = priceService.getLatestPrice(pair);
        if (latestPrice == null) {
            throw new RuntimeException("No price available for " + pair);
        }

        String baseCurrency = pair.replace("USDT", ""); // ETH, BTC
        String quoteCurrency = "USDT";

        BigDecimal price = "BUY".equalsIgnoreCase(type) ? latestPrice.getAsk() : latestPrice.getBid();
        BigDecimal totalCost = price.multiply(amount);

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid price detected");
        }

        if ("BUY".equalsIgnoreCase(type)) {
            Wallet usdtWallet = walletRepository.findByUserIdAndCurrency(userId, quoteCurrency)
                    .orElseThrow(() -> new RuntimeException("Insufficient funds (Wallet not found)"));

            if (usdtWallet.getBalance().compareTo(totalCost) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            usdtWallet.setBalance(usdtWallet.getBalance().subtract(totalCost));
            walletRepository.save(usdtWallet);

            Wallet baseWallet = walletRepository.findByUserIdAndCurrency(userId, baseCurrency)
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
            Wallet baseWallet = walletRepository.findByUserIdAndCurrency(userId, baseCurrency)
                    .orElseThrow(() -> new RuntimeException("Insufficient crypto balance"));

            if (baseWallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient crypto balance");
            }

            baseWallet.setBalance(baseWallet.getBalance().subtract(amount));
            walletRepository.save(baseWallet);

            Wallet usdtWallet = walletRepository.findByUserIdAndCurrency(userId, quoteCurrency)
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

    public List<Wallet> getWalletBalance(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    public List<Transaction> getTransactionHistory(Long userId) {
        return transactionRepository.findByUserId(userId);
    }
}
