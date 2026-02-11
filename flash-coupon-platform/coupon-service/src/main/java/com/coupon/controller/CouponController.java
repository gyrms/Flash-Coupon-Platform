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

    /**
     * Redis-DB 동기화 (운영용)
     * DB 기준으로 Redis 재고를 재설정
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<String> syncStock(@PathVariable Long id) {
        couponService.syncStockFromDB(id);
        return ResponseEntity.ok("동기화 완료 - couponId: " + id);
    }

    /**
     * Redis-DB 일관성 체크 (모니터링용)
     */
    @GetMapping("/{id}/consistency")
    public ResponseEntity<ConsistencyResponse> checkConsistency(@PathVariable Long id) {
        boolean isConsistent = couponService.checkConsistency(id);
        Long redisStock = couponService.getStock(id);
        long dbIssuedCount = couponService.getIssuedCount(id);

        ConsistencyResponse response = new ConsistencyResponse(
                id, isConsistent, redisStock, dbIssuedCount
        );
        return ResponseEntity.ok(response);
    }
}
