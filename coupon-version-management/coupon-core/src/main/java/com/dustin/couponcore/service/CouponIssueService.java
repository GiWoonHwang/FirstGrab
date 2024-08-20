package com.dustin.couponcore.service;

import com.dustin.couponcore.exception.CouponIssueException;
import com.dustin.couponcore.exception.ErrorCode;
import com.dustin.couponcore.model.event.CouponIssueCompleteEvent;
import com.dustin.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.dustin.couponcore.repository.mysql.CouponIssueRepository;
import com.dustin.couponcore.repository.mysql.CouponJpaRepository;
import com.dustin.couponcore.model.Coupon;
import com.dustin.couponcore.model.CouponIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화 합니다.
@RequiredArgsConstructor
// @Service 어노테이션은 이 클래스가 서비스 레이어의 빈(Bean)임을 나타내며, Spring IoC 컨테이너에서 관리됩니다.
@Service
public class CouponIssueService {

    // 쿠폰 정보를 관리하는 JPA 레포지토리입니다.
    private final CouponJpaRepository couponJpaRepository;

    // 쿠폰 발급 정보를 관리하는 JPA 레포지토리입니다.
    private final CouponIssueJpaRepository couponIssueJpaRepository;

    // 쿠폰 발급 정보를 조회하는 커스텀 레포지토리입니다.
    private final CouponIssueRepository couponIssueRepository;

    // Spring 이벤트를 발행하는 컴포넌트로, 특정 이벤트를 다른 컴포넌트에 전달할 때 사용됩니다.
    private final ApplicationEventPublisher applicationEventPublisher;

    // 쿠폰을 발급하는 메서드입니다. 트랜잭션 내에서 실행되며, 쿠폰 발급과 관련된 모든 작업을 처리합니다.
    @Transactional
    public void issue(long couponId, long userId) {
        // 락을 걸어 쿠폰을 조회하고 발급 가능한 상태로 변경합니다.
        Coupon coupon = findCouponWithLock(couponId);
        coupon.issue();

        // 쿠폰 발급 정보를 저장합니다.
        saveCouponIssue(couponId, userId);

        // 쿠폰 발급 완료 이벤트를 발행합니다.
        publishCouponEvent(coupon);
    }

    // 쿠폰을 조회하는 메서드입니다. 트랜잭션이 읽기 전용으로 설정되어 있어 성능을 최적화합니다.
    @Transactional(readOnly = true)
    public Coupon findCoupon(long couponId) {
        // 쿠폰 ID로 쿠폰을 조회하며, 존재하지 않을 경우 예외를 발생시킵니다.
        return couponJpaRepository.findById(couponId).orElseThrow(() -> {
            throw new CouponIssueException(ErrorCode.COUPON_NOT_EXIST,
                    "쿠폰 정책이 존재하지 않습니다. %s".formatted(couponId));
        });
    }

    // 락을 걸어 쿠폰을 조회하는 메서드입니다. 트랜잭션 내에서 실행되며, 데이터 일관성을 보장합니다.
    @Transactional
    public Coupon findCouponWithLock(long couponId) {
        // 쿠폰 ID로 락을 걸어 쿠폰을 조회하며, 존재하지 않을 경우 예외를 발생시킵니다.
        return couponJpaRepository.findCouponWithLock(couponId).orElseThrow(() -> {
            throw new CouponIssueException(ErrorCode.COUPON_NOT_EXIST,
                    "쿠폰 정책이 존재하지 않습니다. %s".formatted(couponId));
        });
    }

    // 쿠폰 발급 정보를 저장하는 메서드입니다. 트랜잭션 내에서 실행됩니다.
    @Transactional
    public CouponIssue saveCouponIssue(long couponId, long userId) {
        // 이미 발급된 쿠폰인지 확인합니다.
        checkAlreadyIssuance(couponId, userId);

        // 쿠폰 발급 정보를 생성합니다.
        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(couponId)
                .userId(userId)
                .build();

        // 쿠폰 발급 정보를 데이터베이스에 저장하고 반환합니다.
        return couponIssueJpaRepository.save(couponIssue);
    }

    // 사용자가 이미 쿠폰을 발급받았는지 확인하는 메서드입니다.
    private void checkAlreadyIssuance(long couponId, long userId) {
        // 특정 쿠폰과 사용자에 대해 발급된 쿠폰이 있는지 조회합니다.
        CouponIssue issue = couponIssueRepository.findFirstCouponIssue(couponId, userId);

        // 이미 발급된 쿠폰이 있을 경우 예외를 발생시킵니다.
        if (issue != null) {
            throw new CouponIssueException(ErrorCode.DUPLICATED_COUPON_ISSUE,
                    "이미 발급된 쿠폰입니다. user_id: %d, coupon_id: %d".formatted(userId, couponId));
        }
    }

    // 쿠폰 발급 완료 이벤트를 발행하는 메서드입니다.
    private void publishCouponEvent(Coupon coupon) {
        // 쿠폰 발급이 완료된 경우에만 이벤트를 발행합니다.
        if (coupon.isIssueComplete()) {
            applicationEventPublisher.publishEvent(new CouponIssueCompleteEvent(coupon.getId()));
        }
    }
}
