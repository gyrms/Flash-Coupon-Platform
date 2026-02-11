package com.coupon.repository;

import com.coupon.domain.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    Optional<CouponIssue> findByCouponIdAndUserId(Long couponId, Long userId);

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    long countByCouponId(Long couponId);
}
