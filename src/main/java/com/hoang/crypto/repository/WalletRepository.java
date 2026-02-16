package com.hoang.crypto.repository;

import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findByUserIdAndCurrency(UUID userId, Currency currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.currency = :currency")
    Optional<Wallet> findByUserIdAndCurrencyWithLock(UUID userId, Currency currency);
}
