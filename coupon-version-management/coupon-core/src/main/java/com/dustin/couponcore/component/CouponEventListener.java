package com.dustin.couponcore.component;

import com.dustin.couponcore.model.event.CouponIssueCompleteEvent;
import com.dustin.couponcore.service.CouponCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화합니다.
@RequiredArgsConstructor
// @Component 어노테이션은 이 클래스가 Spring의 관리되는 빈(Bean)임을 나타내며, Spring IoC 컨테이너에서 사용할 수 있게 합니다.
@Component
public class CouponEventListener {

    // 쿠폰 캐시와 관련된 서비스를 제공하는 클래스입니다.
    private final CouponCacheService couponCacheService;

    // 로깅을 위한 Logger 인스턴스입니다. 이 클래스의 이름을 로깅 메시지에 사용합니다.
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    // @TransactionalEventListener 어노테이션은 트랜잭션 이벤트를 리스닝할 메서드를 정의합니다.
    // phase = TransactionPhase.AFTER_COMMIT 설정은 트랜잭션이 커밋된 후에 메서드가 실행되도록 합니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void issueComplete(CouponIssueCompleteEvent event) {
        // 쿠폰 발급이 완료되었음을 알리는 로그 메시지입니다. 캐시 갱신이 시작됨을 나타냅니다.
        log.info("issue complete. cache refresh start couponId: %s".formatted(event.couponId()));

        // 쿠폰 ID를 사용하여 Redis와 같은 분산 캐시에 쿠폰 데이터를 갱신합니다.
        couponCacheService.putCouponCache(event.couponId());

        // 쿠폰 ID를 사용하여 로컬 캐시에 쿠폰 데이터를 갱신합니다.
        couponCacheService.putCouponLocalCache(event.couponId());

        // 캐시 갱신이 완료되었음을 알리는 로그 메시지입니다.
        log.info("issue complete cache refresh end couponId: %s".formatted(event.couponId()));
    }
}
