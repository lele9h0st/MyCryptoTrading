package com.hoang.crypto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.crypto.constant.CryptoPair;
import com.hoang.crypto.constant.Currency;
import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.entity.Transaction;
import com.hoang.crypto.entity.Wallet;
import com.hoang.crypto.service.PriceService;
import com.hoang.crypto.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CryptoController.class)
class CryptoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceService priceService;

    @MockitoBean
    private TradingService tradingService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    @Qualifier("priceLimitBucket")
    private Bucket priceLimitBucket;

    @MockitoBean
    @Qualifier("tradeLimitBucket")
    private Bucket tradeLimitBucket;

    @BeforeEach
    void setUp() {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(10L);

        when(priceLimitBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(tradeLimitBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    }

    @Test
    void getLatestPrice_Success() throws Exception {
        PriceAggregate price = new PriceAggregate();
        price.setPair(CryptoPair.ETHUSDT);
        price.setBid(new BigDecimal("2000"));
        price.setAsk(new BigDecimal("2100"));
        price.setTimestamp(LocalDateTime.now());

        when(priceService.getLatestPrice(CryptoPair.ETHUSDT)).thenReturn(price);

        mockMvc.perform(get("/api/crypto/price/latest?pair=ETHUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pair").value("ETHUSDT"))
                .andExpect(jsonPath("$.bid").value(2000))
                .andExpect(jsonPath("$.ask").value(2100))
                .andExpect(jsonPath("$.id").doesNotExist()); // Verification that ID is hidden
    }

    @Test
    void executeTrade_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        CryptoController.TradeRequest request = new CryptoController.TradeRequest();
        request.setUserId(userId);
        request.setPair(CryptoPair.ETHUSDT);
        request.setType("BUY");
        request.setAmount(new BigDecimal("1"));

        Transaction transaction = new Transaction();
        transaction.setPair(CryptoPair.ETHUSDT);
        transaction.setType("BUY");
        transaction.setPrice(new BigDecimal("2100"));
        transaction.setAmount(new BigDecimal("1"));
        transaction.setTimestamp(LocalDateTime.now());

        when(tradingService.executeTrade(eq(userId), eq(CryptoPair.ETHUSDT), eq("BUY"), any(BigDecimal.class)))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/crypto/trade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pair").value("ETHUSDT"))
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.price").value(2100))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void getWalletBalance_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setCurrency(Currency.USDT);
        wallet.setBalance(new BigDecimal("5000"));

        when(tradingService.getWalletBalance(userId)).thenReturn(List.of(wallet));

        mockMvc.perform(get("/api/crypto/wallet/balance?userId=" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currency").value("USDT"))
                .andExpect(jsonPath("$[0].balance").value(5000))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void getTransactionHistory_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setPair(CryptoPair.ETHUSDT);
        transaction.setType("BUY");
        transaction.setPrice(new BigDecimal("2100"));
        transaction.setAmount(new BigDecimal("1"));

        when(tradingService.getTransactionHistory(userId)).thenReturn(List.of(transaction));

        mockMvc.perform(get("/api/crypto/history?userId=" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pair").value("ETHUSDT"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }
}
