package com.coupon.service;

import com.coupon.domain.Coupon;
import com.coupon.domain.CouponIssue;
import com.coupon.repository.CouponIssueRepository;
import com.coupon.repository.CouponRepository;
import com.coupon.service.dto.CouponCreateRequest;
import com.coupon.service.dto.CouponIssueRequest;
import com.coupon.service.dto.CouponIssueResponse;
import com.coupon.service.dto.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponStockService couponStockService;

    /**
     * 쿠폰 생성
     */
    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        Coupon coupon = Coupon.builder()
                .name(request.name())
                .totalQuantity(request.totalQuantity())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();

        Coupon saved = couponRepository.save(coupon);

        // Redis에 재고 초기화
        couponStockService.initializeStock(saved.getId(), saved.getTotalQuantity());

        log.info("쿠폰 생성 완료 - id: {}, name: {}, quantity: {}",
                saved.getId(), saved.getName(), saved.getTotalQuantity());

        return CouponResponse.from(saved);
    }

    /**
     * 쿠폰 조회
     */
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다: " + couponId));
        return CouponResponse.from(coupon);
    }

    /**
     * 쿠폰 발급 (핵심 로직)
     * Redis Lua Script로 원자적 재고 감소 후 DB 저장
     * DB 저장 실패 시 Redis 롤백 (보상 로직)
     */
    @Transactional
    public CouponIssueResponse issueCoupon(Long couponId, CouponIssueRequest request) {
        // 1. 쿠폰 존재 여부 확인
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다: " + couponId));

        // 2. 쿠폰 유효 기간 확인
        if (!coupon.isAvailable()) {
            return CouponIssueResponse.fail("쿠폰 발급 기간이 아니거나 품절되었습니다.");
        }

        // 3. Redis에서 원자적 재고 감소 시도
        Long result = couponStockService.decrementStock(couponId, request.userId());

        if (result == -1) {
            return CouponIssueResponse.fail("이미 발급받은 쿠폰입니다.");
        }

        if (result == 0) {
            return CouponIssueResponse.fail("쿠폰이 모두 소진되었습니다.");
        }

        // 4. 성공 시 DB에 발급 내역 저장 (보상 로직 포함)
        try {
            CouponIssue couponIssue = CouponIssue.builder()
                    .couponId(couponId)
                    .userId(request.userId())
                    .build();

            CouponIssue saved = couponIssueRepository.save(couponIssue);

            log.info("쿠폰 발급 성공 - couponId: {}, userId: {}, issueId: {}",
                    couponId, request.userId(), saved.getId());

            return CouponIssueResponse.success(saved.getId(), couponId, request.userId());

        } catch (Exception e) {
            // 5. DB 저장 실패 시 Redis 롤백 (보상 로직)
            log.error("DB 저장 실패, Redis 롤백 시작 - couponId: {}, userId: {}, error: {}",
                    couponId, request.userId(), e.getMessage());

            couponStockService.rollback(couponId, request.userId());

            return CouponIssueResponse.fail("쿠폰 발급 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 잔여 수량 조회
     */
    public Long getStock(Long couponId) {
        return couponStockService.getStock(couponId);
    }

    /**
     * DB에 저장된 발급 내역 수 조회
     */
    public long getIssuedCount(Long couponId) {
        return couponIssueRepository.countByCouponId(couponId);
    }

    /**
     * Redis와 DB 동기화 (DB 기준)
     * Redis 재고 = 총 수량 - DB 발급 내역 수
     */
    public void syncStockFromDB(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다: " + couponId));

        long issuedCount = couponIssueRepository.countByCouponId(couponId);
        long actualStock = coupon.getTotalQuantity() - issuedCount;

        // Redis 재고를 DB 기준으로 재설정
        couponStockService.initializeStock(couponId, (int) actualStock);

        log.info("Redis-DB 동기화 완료 - couponId: {}, totalQuantity: {}, issuedCount: {}, actualStock: {}",
                couponId, coupon.getTotalQuantity(), issuedCount, actualStock);
    }

    /**
     * Redis와 DB 불일치 확인
     * @return true = 일치, false = 불일치
     */
    public boolean checkConsistency(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다: " + couponId));

        long dbIssuedCount = couponIssueRepository.countByCouponId(couponId);
        long redisStock = couponStockService.getStock(couponId);

        long expectedRedisStock = coupon.getTotalQuantity() - dbIssuedCount;

        boolean isConsistent = (redisStock == expectedRedisStock);

        if (!isConsistent) {
            log.warn("Redis-DB 불일치 감지! couponId: {}, Redis 재고: {}, 예상 재고: {}",
                    couponId, redisStock, expectedRedisStock);
        }

        return isConsistent;
    }
}
