package com.coupon;

import com.coupon.domain.Coupon;
import com.coupon.repository.CouponIssueRepository;
import com.coupon.repository.CouponRepository;
import com.coupon.service.CouponService;
import com.coupon.service.CouponStockService;
import com.coupon.service.dto.CouponCreateRequest;
import com.coupon.service.dto.CouponIssueRequest;
import com.coupon.service.dto.CouponIssueResponse;
import com.coupon.service.dto.CouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponStockService couponStockService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @BeforeEach
    void setUp() {
        couponIssueRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 1000명이 100개 쿠폰 발급 요청 시 초과 발급 없음")
    void concurrencyTest_noOverselling() throws InterruptedException {
        // Given: 100개 쿠폰 생성
        int totalQuantity = 100;
        int totalRequests = 1000;

        CouponCreateRequest createRequest = new CouponCreateRequest(
                "선착순 쿠폰",
                totalQuantity,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1)
        );

        CouponResponse coupon = couponService.createCoupon(createRequest);
        Long couponId = coupon.id();

        // When: 1000명이 동시에 요청
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            long userId = i + 1;
            executor.submit(() -> {
                try {
                    CouponIssueRequest issueRequest = new CouponIssueRequest(userId);
                    CouponIssueResponse response = couponService.issueCoupon(couponId, issueRequest);

                    if (response.success()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();

        // Then: 검증
        Long remainingStock = couponStockService.getStock(couponId);
        long issuedCount = couponService.getIssuedCount(couponId);

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("총 요청: " + totalRequests);
        System.out.println("총 쿠폰 수량: " + totalQuantity);
        System.out.println("발급 성공: " + successCount.get());
        System.out.println("발급 실패: " + failCount.get());
        System.out.println("Redis 잔여 재고: " + remainingStock);
        System.out.println("DB 발급 내역 수: " + issuedCount);
        System.out.println("소요 시간: " + (endTime - startTime) + "ms");
        System.out.println("========================");

        // 핵심 검증: 초과 발급 없음
        assertThat(successCount.get()).isEqualTo(totalQuantity);
        assertThat(issuedCount).isEqualTo(totalQuantity);
        assertThat(remainingStock).isEqualTo(0L);
        assertThat(successCount.get() + failCount.get()).isEqualTo(totalRequests);
    }

    @Test
    @DisplayName("동일 사용자 중복 발급 방지")
    void duplicateIssue_prevented() {
        // Given
        CouponCreateRequest createRequest = new CouponCreateRequest(
                "중복 방지 테스트 쿠폰",
                10,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1)
        );

        CouponResponse coupon = couponService.createCoupon(createRequest);
        Long couponId = coupon.id();
        Long userId = 999L;

        // When: 같은 사용자가 두 번 발급 시도
        CouponIssueRequest request = new CouponIssueRequest(userId);
        CouponIssueResponse firstResponse = couponService.issueCoupon(couponId, request);
        CouponIssueResponse secondResponse = couponService.issueCoupon(couponId, request);

        // Then
        assertThat(firstResponse.success()).isTrue();
        assertThat(secondResponse.success()).isFalse();
        assertThat(secondResponse.message()).contains("이미 발급");
    }
}
