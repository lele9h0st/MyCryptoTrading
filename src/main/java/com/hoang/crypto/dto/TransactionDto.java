package com.hoang.crypto.dto;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDto {
    private CryptoPair pair;
    private String type;
    private BigDecimal price;
    private BigDecimal amount;
    private LocalDateTime timestamp;

    public static TransactionDto fromEntity(Transaction entity) {
        if (entity == null)
            return null;
        TransactionDto dto = new TransactionDto();
        dto.setPair(entity.getPair());
        dto.setType(entity.getType());
        dto.setPrice(entity.getPrice());
        dto.setAmount(entity.getAmount());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}
