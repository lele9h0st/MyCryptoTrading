package com.hoang.crypto.entity;

import com.hoang.crypto.constant.CryptoPair;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class PriceAggregate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private CryptoPair pair; // ETHUSDT, BTCUSDT
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime timestamp;
}
