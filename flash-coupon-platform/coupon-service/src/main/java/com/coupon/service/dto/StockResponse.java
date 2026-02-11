package com.coupon.service.dto;

public record StockResponse(
        Long couponId,
        Long remainingStock,
        Long issuedCount
) {
}
