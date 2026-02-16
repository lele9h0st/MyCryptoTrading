package com.hoang.crypto.repository;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.entity.PriceAggregate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PriceAggregateRepository extends JpaRepository<PriceAggregate, UUID> {
    PriceAggregate findFirstByPairOrderByTimestampDesc(CryptoPair pair);
}
