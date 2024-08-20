package com.dustin.couponcore.service;

import com.dustin.couponcore.repository.redis.dto.CouponRedisEntity;
import com.dustin.couponcore.model.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화합니다.
@RequiredArgsConstructor
// @Service 어노테이션은 이 클래스가 서비스 레이어의 빈(Bean)임을 나타내며, Spring IoC 컨테이너에서 관리됩니다.
@Service
public class CouponCacheService {

    // 쿠폰 발급 관련 비즈니스 로직을 처리하는 서비스입니다.
    private final CouponIssueService couponIssueService;

    // 쿠폰 정보를 캐시에서 조회하는 메서드입니다. 캐시 이름은 "coupon"으로 지정됩니다.
    // 캐시에 해당 쿠폰 ID에 대한 데이터가 없을 경우, couponIssueService를 통해 데이터를 조회하고 이를 캐시에 저장합니다.
    @Cacheable(cacheNames = "coupon")
    public CouponRedisEntity getCouponCache(long couponId) {
        Coupon coupon = couponIssueService.findCoupon(couponId);
        return new CouponRedisEntity(coupon);
    }

    // 쿠폰 정보를 캐시에 강제로 업데이트하는 메서드입니다. 캐시 이름은 "coupon"으로 지정됩니다.
    // 이 메서드는 새로운 쿠폰 정보를 캐시에 저장하고, 기존의 캐시 데이터를 덮어씁니다.
    @CachePut(cacheNames = "coupon")
    public CouponRedisEntity putCouponCache(long couponId) {
        return getCouponCache(couponId);
    }

    // 로컬 캐시에서 쿠폰 정보를 조회하는 메서드입니다. 캐시 이름은 "coupon"이며, 로컬 캐시 관리자를 사용합니다.
    // 로컬 캐시에서 데이터가 없을 경우, proxy 메서드를 통해 글로벌 캐시에서 데이터를 조회합니다.
    @Cacheable(cacheNames = "coupon", cacheManager = "localCacheManager")
    public CouponRedisEntity getCouponLocalCache(long couponId) {
        return proxy().getCouponCache(couponId);
    }

    // 로컬 캐시에 쿠폰 정보를 강제로 업데이트하는 메서드입니다. 캐시 이름은 "coupon"이며, 로컬 캐시 관리자를 사용합니다.
    // 이 메서드는 새로운 쿠폰 정보를 로컬 캐시에 저장하고, 기존의 로컬 캐시 데이터를 덮어씁니다.
    @CachePut(cacheNames = "coupon", cacheManager = "localCacheManager")
    public CouponRedisEntity putCouponLocalCache(long couponId) {
        return getCouponLocalCache(couponId);
    }

    // 현재 프록시된 객체를 반환하는 메서드입니다.
    // Spring의 AOP 기반 캐시 어노테이션을 제대로 작동시키기 위해 사용됩니다.
    private CouponCacheService proxy() {
        return ((CouponCacheService) AopContext.currentProxy());
    }
}
