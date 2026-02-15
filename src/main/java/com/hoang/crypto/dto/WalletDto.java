package com.hoang.crypto.dto;

import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.Wallet;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletDto {
    private Currency currency;
    private BigDecimal balance;

    public static WalletDto fromEntity(Wallet entity) {
        if (entity == null)
            return null;
        WalletDto dto = new WalletDto();
        dto.setCurrency(entity.getCurrency());
        dto.setBalance(entity.getBalance());
        return dto;
    }
}
