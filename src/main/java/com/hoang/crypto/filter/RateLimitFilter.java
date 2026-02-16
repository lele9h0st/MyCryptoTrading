package com.hoang.crypto.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Bucket priceLimitBucket;
    private final Bucket tradeLimitBucket;

    public RateLimitFilter(
            @Qualifier("priceLimitBucket") Bucket priceLimitBucket,
            @Qualifier("tradeLimitBucket") Bucket tradeLimitBucket) {
        this.priceLimitBucket = priceLimitBucket;
        this.tradeLimitBucket = tradeLimitBucket;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        Bucket bucket = null;

        if (path.startsWith("/api/crypto/price/latest")) {
            bucket = priceLimitBucket;
        } else if (path.startsWith("/api/crypto/trade")) {
            bucket = tradeLimitBucket;
        }

        if (bucket != null) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
            } else {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests. Please try again later.");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
