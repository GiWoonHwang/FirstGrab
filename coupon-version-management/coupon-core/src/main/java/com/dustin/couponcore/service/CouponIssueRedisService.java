package com.dustin.couponcore.service;

import com.dustin.couponcore.exception.CouponIssueException;
import com.dustin.couponcore.exception.ErrorCode;
import com.dustin.couponcore.repository.redis.RedisRepository;
import com.dustin.couponcore.repository.redis.dto.CouponRedisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.dustin.couponcore.util.CouponRedisUtils.getIssueRequestKey;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화합니다.
@RequiredArgsConstructor
// @Service 어노테이션은 이 클래스가 서비스 레이어의 빈(Bean)임을 나타내며, Spring IoC 컨테이너에서 관리됩니다.
@Service
public class CouponIssueRedisService {

    // Redis와의 상호작용을 담당하는 레포지토리입니다.
    private final RedisRepository redisRepository;

    // 쿠폰 발급 수량을 확인하고, 중복 발급 및 발급 가능한 수량을 초과하는지 여부를 검증하는 메서드입니다.
    public void checkCouponIssueQuantity(CouponRedisEntity coupon, long userId) {
        // 사용자가 쿠폰을 중복 발급받았는지 확인합니다.
        if (!availableUserIssueQuantity(coupon.id(), userId)) {
            throw new CouponIssueException(ErrorCode.DUPLICATED_COUPON_ISSUE,
                    "발급 가능한 수량을 초과합니다. couponId : %s, userId: %s".formatted(coupon.id(), userId));
        }
        // 총 발급 가능한 쿠폰 수량을 초과하지 않았는지 확인합니다.
        if (!availableTotalIssueQuantity(coupon.totalQuantity(), coupon.id())) {
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY,
                    "발급 가능한 수량을 초과합니다. couponId : %s, userId : %s".formatted(coupon.id(), userId));
        }
    }

    // 주어진 쿠폰 ID와 관련된 전체 발급 가능한 수량을 확인하는 메서드입니다.
    // totalQuantity가 null이면 무제한 발급 가능으로 간주하며, 그렇지 않으면 Redis에 저장된 현재 발급 수와 비교합니다.
    public boolean availableTotalIssueQuantity(Integer totalQuantity, long couponId) {
        if (totalQuantity == null) {
            return true;  // null이면 무제한 발급 가능
        }
        String key = getIssueRequestKey(couponId);
        return totalQuantity > redisRepository.sCard(key);  // Redis에 저장된 발급 수와 비교
    }

    // 특정 사용자가 해당 쿠폰을 이미 발급받았는지 여부를 확인하는 메서드입니다.
    public boolean availableUserIssueQuantity(long couponId, long userId) {
        String key = getIssueRequestKey(couponId);
        return !redisRepository.sIsMember(key, String.valueOf(userId));  // Redis에서 사용자 ID가 존재하는지 확인
    }
}
