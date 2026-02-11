package com.coupon.controller;

import com.coupon.service.CouponService;
import com.coupon.service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 쿠폰 생성
     */
    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            @Valid @RequestBody CouponCreateRequest request) {
        CouponResponse response = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 쿠폰 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long id) {
        CouponResponse response = couponService.getCoupon(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 쿠폰 발급 (핵심 API)
     */
    @PostMapping("/{id}/issue")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponIssueRequest request) {
        CouponIssueResponse response = couponService.issueCoupon(id, request);

        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    /**
     * 잔여 수량 조회
     */
    @GetMapping("/{id}/stock")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long id) {
        Long remainingStock = couponService.getStock(id);
        long issuedCount = couponService.getIssuedCount(id);

        StockResponse response = new StockResponse(id, remainingStock, issuedCount);
        return ResponseEntity.ok(response);
    }
}
