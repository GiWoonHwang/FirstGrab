package com.dustin.couponcore.service;

import com.dustin.couponcore.component.DistributeLockExecutor;
import com.dustin.couponcore.exception.CouponIssueException;
import com.dustin.couponcore.exception.ErrorCode;
import com.dustin.couponcore.repository.redis.RedisRepository;
import com.dustin.couponcore.repository.redis.dto.CouponIssueRequest;
import com.dustin.couponcore.repository.redis.dto.CouponRedisEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.dustin.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static com.dustin.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화합니다.
@RequiredArgsConstructor
// @Service 어노테이션은 이 클래스가 서비스 레이어의 빈(Bean)임을 나타내며, Spring IoC 컨테이너에서 관리됩니다.
@Service
public class AsyncCouponIssueServiceV1 {

    // Redis와의 상호작용을 담당하는 레포지토리입니다.
    private final RedisRepository redisRepository;

    // 쿠폰 발급과 관련된 Redis 작업을 처리하는 서비스입니다.
    private final CouponIssueRedisService couponIssueRedisService;

    // 분산 락을 관리하는 컴포넌트로, 동시성 제어를 위해 사용됩니다.
    private final DistributeLockExecutor distributeLockExecutor;

    // 쿠폰 캐시를 관리하는 서비스입니다.
    private final CouponCacheService couponCacheService;

    // JSON 직렬화 및 역직렬화를 담당하는 ObjectMapper입니다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 쿠폰 발급을 처리하는 메서드입니다.
    public void issue(long couponId, long userId) {
        // 쿠폰 캐시에서 쿠폰 정보를 가져옵니다.
        CouponRedisEntity coupon = couponCacheService.getCouponCache(couponId);

        // 쿠폰이 발급 가능한 상태인지 확인합니다.
        coupon.checkIssuableCoupon();

        // 쿠폰 ID를 기반으로 락을 획득하여 발급 프로세스를 동기화합니다.
        distributeLockExecutor.execute("lock_%s".formatted(couponId), 3000, 3000, () -> {
            // 쿠폰 발급 수량을 확인하고 중복 발급을 방지합니다.
            couponIssueRedisService.checkCouponIssueQuantity(coupon, userId);

            // 쿠폰 발급 요청을 처리합니다.
            issueRequest(couponId, userId);
        });
    }

    // 쿠폰 발급 요청을 Redis에 저장하는 메서드입니다.
    private void issueRequest(long couponId, long userId) {
        // 쿠폰 발급 요청 정보를 담은 객체를 생성합니다.
        CouponIssueRequest issueRequest = new CouponIssueRequest(couponId, userId);
        try {
            // 쿠폰 발급 요청 객체를 JSON 문자열로 변환합니다.
            String value = objectMapper.writeValueAsString(issueRequest);

            // 쿠폰 발급 요청을 Redis의 집합(Set)과 큐(Queue)에 저장합니다.
            redisRepository.sAdd(getIssueRequestKey(couponId), String.valueOf(userId));
            redisRepository.rPush(getIssueRequestQueueKey(), value);
        } catch (JsonProcessingException e) {
            // JSON 변환 중 오류가 발생하면 예외를 던집니다.
            throw new CouponIssueException(ErrorCode.FAIL_COUPON_ISSUE_REQUEST, "input: %s".formatted(issueRequest));
        }
    }
}
