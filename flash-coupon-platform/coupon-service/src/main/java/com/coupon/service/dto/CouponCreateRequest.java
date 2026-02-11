package com.coupon.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CouponCreateRequest(
        @NotBlank(message = "쿠폰명은 필수입니다")
        String name,

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        Integer totalQuantity,

        @NotNull(message = "시작일시는 필수입니다")
        LocalDateTime startAt,

        @NotNull(message = "종료일시는 필수입니다")
        LocalDateTime endAt
) {
}
