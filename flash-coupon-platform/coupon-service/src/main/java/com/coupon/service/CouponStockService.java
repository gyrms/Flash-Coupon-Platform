package com.coupon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponStockService {

    private static final String STOCK_KEY_PREFIX = "coupon:stock:";
    private static final String ISSUED_KEY_PREFIX = "coupon:issued:";

    private final RedisTemplate<String, Long> redisTemplate;
    private final DefaultRedisScript<Long> stockDecrementScript;

    /**
     * Redis에 쿠폰 재고 초기화
     */
    public void initializeStock(Long couponId, Integer quantity) {
        String stockKey = STOCK_KEY_PREFIX + couponId;
        redisTemplate.opsForValue().set(stockKey, quantity.longValue());
        log.info("쿠폰 재고 초기화 - couponId: {}, quantity: {}", couponId, quantity);
    }

    /**
     * Lua Script를 통한 원자적 재고 감소
     * @return 1: 성공, 0: 재고 없음, -1: 이미 발급됨
     */
    public Long decrementStock(Long couponId, Long userId) {
        String stockKey = STOCK_KEY_PREFIX + couponId;
        String issuedKey = ISSUED_KEY_PREFIX + couponId;

        Long result = redisTemplate.execute(
                stockDecrementScript,
                Arrays.asList(stockKey, issuedKey),
                userId.toString()
        );

        log.debug("재고 감소 시도 - couponId: {}, userId: {}, result: {}", couponId, userId, result);
        return result;
    }

    /**
     * 현재 남은 재고 조회
     */
    public Long getStock(Long couponId) {
        String stockKey = STOCK_KEY_PREFIX + couponId;
        Long stock = redisTemplate.opsForValue().get(stockKey);
        return stock != null ? stock : 0L;
    }

    /**
     * 사용자가 이미 발급받았는지 확인
     */
    public boolean isAlreadyIssued(Long couponId, Long userId) {
        String issuedKey = ISSUED_KEY_PREFIX + couponId;
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(issuedKey, userId.toString())
        );
    }

    /**
     * 재고 복구 (보상 로직용)
     * DB 저장 실패 시 Redis 재고를 다시 증가시킴
     */
    public void incrementStock(Long couponId) {
        String stockKey = STOCK_KEY_PREFIX + couponId;
        redisTemplate.opsForValue().increment(stockKey);
        log.warn("재고 복구 - couponId: {}", couponId);
    }

    /**
     * 발급 목록에서 사용자 제거 (보상 로직용)
     * DB 저장 실패 시 Redis에서 사용자를 제거함
     */
    public void removeIssuedUser(Long couponId, Long userId) {
        String issuedKey = ISSUED_KEY_PREFIX + couponId;
        redisTemplate.opsForSet().remove(issuedKey, userId.toString());
        log.warn("발급 목록에서 사용자 제거 - couponId: {}, userId: {}", couponId, userId);
    }

    /**
     * Redis 롤백 (재고 복구 + 사용자 제거)
     */
    public void rollback(Long couponId, Long userId) {
        incrementStock(couponId);
        removeIssuedUser(couponId, userId);
        log.warn("Redis 롤백 완료 - couponId: {}, userId: {}", couponId, userId);
    }
}
