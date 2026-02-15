package com.hoang.crypto.service;

import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.repository.PriceAggregateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private PriceAggregateRepository priceAggregateRepository;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PriceService priceService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        priceService = new PriceService(priceAggregateRepository, restClientBuilder);
    }

    @Test
    void fetchAndAggregatePrices_SuccessfulAggregation() {
        // Arrange
        PriceService.BinanceTicker ethBinance = new PriceService.BinanceTicker();
        ethBinance.setSymbol("ETHUSDT");
        ethBinance.setBidPrice(new BigDecimal("2000"));
        ethBinance.setAskPrice(new BigDecimal("2100"));

        PriceService.BinanceTicker btcBinance = new PriceService.BinanceTicker();
        btcBinance.setSymbol("BTCUSDT");
        btcBinance.setBidPrice(new BigDecimal("40000"));
        btcBinance.setAskPrice(new BigDecimal("41000"));

        PriceService.BinanceTicker[] binanceTickers = { ethBinance, btcBinance };

        PriceService.HuobiTicker ethHuobi = new PriceService.HuobiTicker();
        ethHuobi.setSymbol("ethusdt");
        ethHuobi.setBid(new BigDecimal("2010"));
        ethHuobi.setAsk(new BigDecimal("2090"));

        PriceService.HuobiTicker btcHuobi = new PriceService.HuobiTicker();
        btcHuobi.setSymbol("btcusdt");
        btcHuobi.setBid(new BigDecimal("40100"));
        btcHuobi.setAsk(new BigDecimal("40900"));

        PriceService.HuobiResponse huobiResponse = new PriceService.HuobiResponse();
        huobiResponse.setData(List.of(ethHuobi, btcHuobi));

        // Mock RestClient calls
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Match specific URLs
        when(requestHeadersUriSpec.uri("https://api.binance.com/api/v3/ticker/bookTicker"))
                .thenReturn(requestHeadersSpec);
        when(responseSpec.body(PriceService.BinanceTicker[].class)).thenReturn(binanceTickers);

        when(requestHeadersUriSpec.uri("https://api.huobi.pro/market/tickers")).thenReturn(requestHeadersSpec);
        when(responseSpec.body(PriceService.HuobiResponse.class)).thenReturn(huobiResponse);

        // Act
        priceService.fetchAndAggregatePrices();

        // Assert
        ArgumentCaptor<PriceAggregate> captor = ArgumentCaptor.forClass(PriceAggregate.class);
        // Should save twice (ETH and BTC)
        verify(priceAggregateRepository, timeout(1000).times(2)).save(captor.capture());

        List<PriceAggregate> savedPrices = captor.getAllValues();
        PriceAggregate ethSaved = savedPrices.stream().filter(p -> p.getPair() == CryptoPair.ETHUSDT).findFirst()
                .orElseThrow();
        PriceAggregate btcSaved = savedPrices.stream().filter(p -> p.getPair() == CryptoPair.BTCUSDT).findFirst()
                .orElseThrow();

        // ETH: Binance(2000, 2100), Huobi(2010, 2090) -> Best: Bid=2010 (Max), Ask=2090
        // (Min)
        assertEquals(0, new BigDecimal("2010").compareTo(ethSaved.getBid()));
        assertEquals(0, new BigDecimal("2090").compareTo(ethSaved.getAsk()));

        // BTC: Binance(40000, 41000), Huobi(40100, 40900) -> Best: Bid=40100 (Max),
        // Ask=40900 (Min)
        assertEquals(0, new BigDecimal("40100").compareTo(btcSaved.getBid()));
        assertEquals(0, new BigDecimal("40900").compareTo(btcSaved.getAsk()));
    }

    @Test
    void getLatestPrice_ReturnsFromRepository() {
        // Arrange
        PriceAggregate price = new PriceAggregate();
        price.setPair(CryptoPair.ETHUSDT);
        when(priceAggregateRepository.findFirstByPairOrderByTimestampDesc(CryptoPair.ETHUSDT)).thenReturn(price);

        // Act
        PriceAggregate result = priceService.getLatestPrice(CryptoPair.ETHUSDT);

        // Assert
        assertNotNull(result);
        assertEquals(CryptoPair.ETHUSDT, result.getPair());
        verify(priceAggregateRepository).findFirstByPairOrderByTimestampDesc(CryptoPair.ETHUSDT);
    }
}
