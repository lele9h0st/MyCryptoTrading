package com.hoang.crypto.controller;

import com.hoang.crypto.entity.PriceAggregate;
import com.hoang.crypto.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final PriceService priceService;

    @GetMapping("/price/latest")
    public ResponseEntity<PriceAggregate> getLatestPrice(@RequestParam String pair) {
        PriceAggregate price = priceService.getLatestPrice(pair);
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(price);
    }

}
