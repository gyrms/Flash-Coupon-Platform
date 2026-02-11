package com.coupon.service.dto;

public record ConsistencyResponse(
        Long couponId,
        boolean isConsistent,
        Long redisStock,
        Long dbIssuedCount
) {
}
