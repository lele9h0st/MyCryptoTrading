package com.hoang.crypto.entity;

import com.hoang.crypto.constant.CryptoPair;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String type; // BUY, SELL

    @Enumerated(EnumType.STRING)
    private CryptoPair pair; // ETHUSDT, BTCUSDT
    private BigDecimal price;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
