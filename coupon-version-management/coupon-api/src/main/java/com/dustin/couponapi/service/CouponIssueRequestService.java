package com.dustin.couponapi.service;

import com.dustin.couponapi.controller.dto.CouponIssueRequestDto;
import com.dustin.couponcore.component.DistributeLockExecutor;
import com.dustin.couponcore.service.AsyncCouponIssueServiceV1;
import com.dustin.couponcore.service.AsyncCouponIssueServiceV2;
import com.dustin.couponcore.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// @RequiredArgsConstructor 어노테이션은 final이 붙은 모든 필드에 대해 생성자를 자동으로 생성해 줍니다.
// 이를 통해 의존성 주입 시에 명시적인 생성자 코드를 작성할 필요가 없어집니다.
@RequiredArgsConstructor
// @Service 어노테이션은 이 클래스가 서비스 레이어의 컴포넌트임을 나타내며, Spring에 의해 빈으로 등록됩니다.
@Service
public class CouponIssueRequestService {

    // 쿠폰 발급을 처리하는 비즈니스 로직을 담고 있는 서비스 클래스입니다.
    private final CouponIssueService couponIssueService;

    // 비동기 방식으로 쿠폰 발급을 처리하는 첫 번째 버전의 서비스 클래스입니다.
    private final AsyncCouponIssueServiceV1 asyncCouponIssueServiceV1;

    // 비동기 방식으로 쿠폰 발급을 처리하는 두 번째 버전의 서비스 클래스입니다.
    private final AsyncCouponIssueServiceV2 asyncCouponIssueServiceV2;

    // 분산 락을 처리하는 컴포넌트로, 동시성 문제를 해결하기 위해 사용됩니다.
    private final DistributeLockExecutor distributeLockExecutor;

    // 로깅을 위한 Logger 인스턴스입니다. 이 클래스의 이름을 로깅 메시지에 사용합니다.
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    // 쿠폰 발급 요청을 처리하는 메서드입니다. 동기 방식으로 쿠폰을 발급합니다.
    public void issueRequestV1(CouponIssueRequestDto requestDto) {
        // 쿠폰 발급 서비스의 issue 메서드를 호출하여 쿠폰을 발급합니다.
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());

        // 발급이 완료된 후, 로그를 남깁니다. 로그에는 발급된 쿠폰 ID와 사용자 ID가 포함됩니다.
        log.info("쿠폰 발급 완료. couponId: %s, userId: %s".formatted(requestDto.couponId(), requestDto.userId()));
    }

    // 비동기 방식으로 쿠폰 발급 요청을 처리하는 메서드입니다. 첫 번째 버전을 사용합니다.
    public void asyncIssueRequestV1(CouponIssueRequestDto requestDto) {
        // 비동기 쿠폰 발급 서비스의 issue 메서드를 호출하여 비동기적으로 쿠폰을 발급합니다.
        asyncCouponIssueServiceV1.issue(requestDto.couponId(), requestDto.userId());
    }

    // 비동기 방식으로 쿠폰 발급 요청을 처리하는 메서드입니다. 두 번째 버전을 사용합니다.
    public void asyncIssueRequestV2(CouponIssueRequestDto requestDto) {
        // 비동기 쿠폰 발급 서비스의 issue 메서드를 호출하여 비동기적으로 쿠폰을 발급합니다.
        asyncCouponIssueServiceV2.issue(requestDto.couponId(), requestDto.userId());
    }
}