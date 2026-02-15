package com.hoang.crypto.controller;

import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.Transaction;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.service.PriceService;
import com.hoang.crypto.service.TradingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final PriceService priceService;
    private final TradingService tradingService;

    @GetMapping("/price/latest")
    public ResponseEntity<PriceAggregate> getLatestPrice(@RequestParam String pair) {
        PriceAggregate price = priceService.getLatestPrice(pair);
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(price);
    }

    @PostMapping("/trade")
    public ResponseEntity<Transaction> executeTrade(@RequestBody TradeRequest request) {
        try {
            Transaction transaction = tradingService.executeTrade(
                    request.getUserId(),
                    request.getPair(),
                    request.getType(),
                    request.getAmount());
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<List<Wallet>> getWalletBalance(@RequestParam Long userId) {
        return ResponseEntity.ok(tradingService.getWalletBalance(userId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@RequestParam Long userId) {
        return ResponseEntity.ok(tradingService.getTransactionHistory(userId));
    }

    @Data
    static class TradeRequest {
        private Long userId;
        private String pair;
        private String type; // BUY, SELL
        private BigDecimal amount;
    }
}
