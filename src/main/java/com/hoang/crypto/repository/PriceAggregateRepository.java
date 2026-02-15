package com.hoang.crypto.repository;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.entity.PriceAggregate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceAggregateRepository extends JpaRepository<PriceAggregate, Long> {
    PriceAggregate findFirstByPairOrderByTimestampDesc(CryptoPair pair);
}
