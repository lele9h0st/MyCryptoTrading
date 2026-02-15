package com.hoang.crypto.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class PriceAggregate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pair; // ETHUSDT, BTCUSDT
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime timestamp;
}
