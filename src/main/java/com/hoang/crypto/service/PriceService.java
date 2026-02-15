package com.hoang.crypto.service;

import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.repository.PriceAggregateRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PriceService {

    private final PriceAggregateRepository priceAggregateRepository;
    private final RestClient restClient;

    private static final String BINANCE_URL = "https://api.binance.com/api/v3/ticker/bookTicker";
    private static final String HUOBI_URL = "https://api.huobi.pro/market/tickers";

    private static final List<String> SUPPORTED_PAIRS = Arrays.asList("ETHUSDT", "BTCUSDT");

    public PriceService(PriceAggregateRepository priceAggregateRepository, RestClient.Builder restClientBuilder) {
        this.priceAggregateRepository = priceAggregateRepository;
        this.restClient = restClientBuilder.build();
    }

    @Scheduled(fixedRate = 10000)
    public void fetchAndAggregatePrices() {
        log.info("Fetching prices...");
        try {
            CompletableFuture<Map<String, BinanceTicker>> binanceFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    BinanceTicker[] tickers = restClient.get()
                            .uri(BINANCE_URL)
                            .retrieve()
                            .body(BinanceTicker[].class);

                    if (tickers == null) {
                        return new HashMap<>();
                    }

                    return Arrays.stream(tickers)
                            .filter(t -> SUPPORTED_PAIRS.contains(t.getSymbol()))
                            .collect(Collectors.toMap(BinanceTicker::getSymbol, t -> t));
                } catch (Exception e) {
                    log.error("Error fetching from Binance", e);
                    return Map.of();
                }
            });

            CompletableFuture<Map<String, HuobiTicker>> huobiFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    HuobiResponse response = restClient.get()
                            .uri(HUOBI_URL)
                            .retrieve()
                            .body(HuobiResponse.class);

                    if (response == null || response.getData() == null)
                        return Map.of();

                    return response.getData().stream()
                            .filter(t -> SUPPORTED_PAIRS.contains(t.getSymbol().toUpperCase()))
                            .collect(Collectors.toMap(t -> t.getSymbol().toUpperCase(), t -> t));
                } catch (Exception e) {
                    log.error("Error fetching from Huobi", e);
                    return Map.of();
                }
            });

            CompletableFuture.allOf(binanceFuture, huobiFuture).join();

            Map<String, BinanceTicker> binanceMap = binanceFuture.get();
            Map<String, HuobiTicker> huobiMap = huobiFuture.get();

            LocalDateTime now = LocalDateTime.now();

            for (String pair : SUPPORTED_PAIRS) {
                BigDecimal bestBid = BigDecimal.ZERO;
                BigDecimal bestAsk = BigDecimal.valueOf(Double.MAX_VALUE);

                // Check Binance
                if (binanceMap.containsKey(pair)) {
                    BinanceTicker b = binanceMap.get(pair);
                    if (b.getBidPrice() != null)
                        bestBid = b.getBidPrice().max(bestBid);
                    if (b.getAskPrice() != null)
                        bestAsk = b.getAskPrice().min(bestAsk);
                }

                // Check Huobi
                if (huobiMap.containsKey(pair)) {
                    HuobiTicker h = huobiMap.get(pair);
                    if (h.getBid() != null)
                        bestBid = h.getBid().max(bestBid);
                    if (h.getAsk() != null)
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
//                    log.info("Saved price for {}: Bid={}, Ask={}", pair, bestBid, bestAsk);
                }
            }

        } catch (Exception e) {
            log.error("Error aggregating prices", e);
        }
    }

    public PriceAggregate getLatestPrice(String pair) {
        return priceAggregateRepository.findLatestByPair(pair);
    }

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
