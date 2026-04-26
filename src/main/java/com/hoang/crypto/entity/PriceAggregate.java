package com.hoang.crypto.entity;

import com.hoang.crypto.constant.CryptoPair;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = PriceAggregate.TABLE_NAME)
public class PriceAggregate {
    public static final String TABLE_NAME = "T_PRICE_AGGREGATE";
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private CryptoPair pair; // ETHUSDT, BTCUSDT
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime timestamp;
}
