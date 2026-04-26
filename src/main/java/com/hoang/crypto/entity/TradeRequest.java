package com.hoang.crypto.entity;

import com.hoang.crypto.constant.CryptoPair;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {
    private CryptoPair pair;
    private String type; // BUY, SELL
    private BigDecimal amount;
}