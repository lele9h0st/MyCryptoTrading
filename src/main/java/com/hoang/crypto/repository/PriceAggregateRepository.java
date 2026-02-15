package com.hoang.crypto.repository;

import com.hoang.crypto.entity.PriceAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PriceAggregateRepository extends JpaRepository<PriceAggregate, Long> {
    @Query(value = "SELECT * FROM price_aggregate WHERE pair = ?1 ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    PriceAggregate findLatestByPair(String pair);
}
