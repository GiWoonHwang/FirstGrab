package com.dustin.couponcore.service;

import com.dustin.couponcore.repository.redis.RedisRepository;
import com.dustin.couponcore.repository.redis.dto.CouponRedisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화합니다.
@RequiredArgsConstructor
// @Service 어노테이션은 이 클래스가 서비스 레이어의 빈(Bean)임을 나타내며, Spring IoC 컨테이너에서 관리됩니다.
@Service
public class AsyncCouponIssueServiceV2 {

    // Redis와의 상호작용을 담당하는 레포지토리입니다.
    private final RedisRepository redisRepository;

    // 쿠폰 캐시를 관리하는 서비스입니다.
    private final CouponCacheService couponCacheService;

    // 쿠폰 발급을 처리하는 메서드입니다.
    public void issue(long couponId, long userId) {
        // 로컬 캐시에서 쿠폰 정보를 가져옵니다.
        CouponRedisEntity coupon = couponCacheService.getCouponLocalCache(couponId);

        // 쿠폰이 발급 가능한 상태인지 확인합니다.
        coupon.checkIssuableCoupon();

        // 쿠폰 발급 요청을 처리합니다.
        issueRequest(couponId, userId, coupon.totalQuantity());
    }

    // 쿠폰 발급 요청을 Redis에 저장하는 메서드입니다.
    public void issueRequest(long couponId, long userCouponId, Integer totalIssueQuantity) {
        // 총 발급 가능 수량이 null인 경우, 무제한 발급 가능으로 간주합니다.
        if (totalIssueQuantity == null) {
            redisRepository.issueRequest(couponId, userCouponId, Integer.MAX_VALUE);
        } else {
            // 총 발급 가능 수량을 기준으로 쿠폰 발급 요청을 Redis에 저장합니다.
            redisRepository.issueRequest(couponId, userCouponId, totalIssueQuantity);
        }
    }
}
