package com.coupon.service.dto;

import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId
) {
}
