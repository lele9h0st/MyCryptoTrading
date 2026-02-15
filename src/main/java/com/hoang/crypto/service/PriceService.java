package com.hoang.crypto.service;

import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.repository.PriceAggregateRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    private final PriceAggregateRepository priceAggregateRepository;
    private final RestTemplate restTemplate;

    private static final String BINANCE_URL = "https://api.binance.com/api/v3/ticker/bookTicker";
    private static final String HUOBI_URL = "https://api.huobi.pro/market/tickers";

    private static final List<String> SUPPORTED_PAIRS = Arrays.asList("ETHUSDT", "BTCUSDT");

    @Scheduled(fixedRate = 10000)
    public void fetchAndAggregatePrices() {
        log.info("Fetching prices...");
        try {
            // Fetch from Binance
            BinanceTicker[] binanceTickers = restTemplate.getForObject(BINANCE_URL, BinanceTicker[].class);
            Map<String, BinanceTicker> binanceMap = Arrays.stream(binanceTickers)
                    .filter(t -> SUPPORTED_PAIRS.contains(t.getSymbol()))
                    .collect(Collectors.toMap(BinanceTicker::getSymbol, t -> t));

            // Fetch from Huobi
            HuobiResponse huobiResponse = restTemplate.getForObject(HUOBI_URL, HuobiResponse.class);
            Map<String, HuobiTicker> huobiMap = huobiResponse.getData().stream()
                    .filter(t -> SUPPORTED_PAIRS.contains(t.getSymbol().toUpperCase()))
                    .collect(Collectors.toMap(t -> t.getSymbol().toUpperCase(), t -> t));

            LocalDateTime now = LocalDateTime.now();

            for (String pair : SUPPORTED_PAIRS) {
                BigDecimal bestBid = BigDecimal.ZERO;
                BigDecimal bestAsk = BigDecimal.valueOf(Double.MAX_VALUE); // Initialize with a high value

                // Check Binance
                if (binanceMap.containsKey(pair)) {
                    BinanceTicker b = binanceMap.get(pair);
                    bestBid = b.getBidPrice().max(bestBid);
                    bestAsk = b.getAskPrice().min(bestAsk);
                }

                // Check Huobi
                if (huobiMap.containsKey(pair)) {
                    HuobiTicker h = huobiMap.get(pair);
                    bestBid = h.getBid().max(bestBid);
                    bestAsk = h.getAsk().min(bestAsk);
                }

                if (bestBid.compareTo(BigDecimal.ZERO) > 0
                        && bestAsk.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) < 0) {
                    PriceAggregate priceAggregate = new PriceAggregate();
                    priceAggregate.setPair(pair);
                    priceAggregate.setBid(bestBid);
                    priceAggregate.setAsk(bestAsk);
                    priceAggregate.setTimestamp(now);
                    priceAggregateRepository.save(priceAggregate);
                    log.info("Saved price for {}: Bid={}, Ask={}", pair, bestBid, bestAsk);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching prices", e);
        }
    }

    public PriceAggregate getLatestPrice(String pair) {
        return priceAggregateRepository.findLatestByPair(pair);
    }

    // Internal DTOs for JSON parsing
    @Data
    static class BinanceTicker {
        private String symbol;
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
    }

    @Data
    static class HuobiResponse {
        private List<HuobiTicker> data;
    }

    @Data
    static class HuobiTicker {
        private String symbol;
        private BigDecimal bid;
        private BigDecimal ask;
    }
}
