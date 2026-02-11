package com.coupon.service.dto;

import com.coupon.domain.Coupon;

import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String name,
        Integer totalQuantity,
        Integer remainingQuantity,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getTotalQuantity(),
                coupon.getRemainingQuantity(),
                coupon.getStartAt(),
                coupon.getEndAt(),
                coupon.getCreatedAt()
        );
    }
}
