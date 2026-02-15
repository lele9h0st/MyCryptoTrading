package com.hoang.crypto.entity;

import com.hoang.crypto.constant.CryptoPair;
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

    @Enumerated(EnumType.STRING)
    private CryptoPair pair; // ETHUSDT, BTCUSDT
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime timestamp;
}
