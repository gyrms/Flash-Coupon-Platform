package com.coupon.service.dto;

public record CouponIssueResponse(
        boolean success,
        String message,
        Long issueId,
        Long couponId,
        Long userId
) {
    public static CouponIssueResponse success(Long issueId, Long couponId, Long userId) {
        return new CouponIssueResponse(true, "쿠폰 발급 성공", issueId, couponId, userId);
    }

    public static CouponIssueResponse fail(String message) {
        return new CouponIssueResponse(false, message, null, null, null);
    }
}
