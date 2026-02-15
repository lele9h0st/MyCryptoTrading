package com.hoang.crypto.controller;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.dto.PriceAggregateDto;
import com.hoang.crypto.dto.TransactionDto;
import com.hoang.crypto.dto.WalletDto;
import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.Transaction;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.exception.InvalidInputException;
import com.hoang.crypto.service.PriceService;
import com.hoang.crypto.service.TradingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final PriceService priceService;
    private final TradingService tradingService;

    @GetMapping("/price/latest")
    public ResponseEntity<PriceAggregateDto> getLatestPrice(@RequestParam CryptoPair pair) {
        PriceAggregate price = priceService.getLatestPrice(pair);
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(PriceAggregateDto.fromEntity(price));
    }

    @PostMapping("/trade")
    public ResponseEntity<TransactionDto> executeTrade(@RequestBody TradeRequest request) {
        try {
            Transaction transaction = tradingService.executeTrade(
                    request.getUserId(),
                    request.getPair(),
                    request.getType(),
                    request.getAmount());
            return ResponseEntity.ok(TransactionDto.fromEntity(transaction));
        } catch (Exception e) {
            throw new InvalidInputException(e.getMessage());
        }
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<List<WalletDto>> getWalletBalance(@RequestParam Long userId) {
        List<Wallet> wallets = tradingService.getWalletBalance(userId);
        List<WalletDto> dtos = wallets.stream()
                .map(WalletDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionDto>> getTransactionHistory(@RequestParam Long userId) {
        List<Transaction> transactions = tradingService.getTransactionHistory(userId);
        List<TransactionDto> dtos = transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Data
    static class TradeRequest {
        private Long userId;
        private CryptoPair pair;
        private String type; // BUY, SELL
        private BigDecimal amount;
    }
}
