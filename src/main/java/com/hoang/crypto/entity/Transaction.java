package com.hoang.crypto.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String type; // BUY, SELL
    private String pair; // ETHUSDT, BTCUSDT
    private BigDecimal price;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
