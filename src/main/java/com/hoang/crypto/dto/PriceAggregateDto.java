package com.hoang.crypto.dto;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.entity.PriceAggregate;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceAggregateDto {
    private CryptoPair pair;
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime timestamp;

    public static PriceAggregateDto fromEntity(PriceAggregate entity) {
        if (entity == null)
            return null;
        PriceAggregateDto dto = new PriceAggregateDto();
        dto.setPair(entity.getPair());
        dto.setBid(entity.getBid());
        dto.setAsk(entity.getAsk());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}
