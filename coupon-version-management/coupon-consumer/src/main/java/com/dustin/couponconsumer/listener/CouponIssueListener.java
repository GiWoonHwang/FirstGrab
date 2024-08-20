package com.dustin.couponconsumer.listener;

import com.dustin.couponcore.repository.redis.RedisRepository;
import com.dustin.couponcore.repository.redis.dto.CouponIssueRequest;
import com.dustin.couponcore.service.CouponIssueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.dustin.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;

// @RequiredArgsConstructor 어노테이션은 final이 붙은 모든 필드에 대해 생성자를 자동으로 생성합니다.
// 이를 통해 의존성 주입 시 명시적인 생성자 코드를 작성하지 않아도 됩니다.
@RequiredArgsConstructor
// @EnableScheduling 어노테이션은 스케줄링 작업을 활성화시켜 주기적으로 메서드를 실행할 수 있도록 합니다.
@EnableScheduling
// @Component 어노테이션은 이 클래스가 Spring의 관리되는 빈(Bean)임을 나타냅니다.
@Component
public class CouponIssueListener {

    // 쿠폰 발급을 처리하는 비즈니스 로직을 담고 있는 서비스 클래스입니다.
    private final CouponIssueService couponIssueService;

    // Redis와의 상호작용을 담당하는 레포지토리입니다.
    private final RedisRepository redisRepository;

    // JSON 데이터의 직렬화 및 역직렬화를 담당하는 ObjectMapper입니다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 쿠폰 발급 요청이 담긴 Redis 큐의 키 값을 저장합니다.
    private final String issueRequestQueueKey = getIssueRequestQueueKey();

    // 로깅을 위한 Logger 인스턴스입니다. 이 클래스의 이름을 로깅 메시지에 사용합니다.
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    // @Scheduled 어노테이션은 일정 주기로 메서드를 실행하도록 합니다.
    // fixedDelay = 1000 설정은 메서드가 완료된 후 1초 뒤에 다시 실행되도록 설정합니다.
    @Scheduled(fixedDelay = 1000)
    public void issue() throws JsonProcessingException {
        // 큐에서 발급 요청을 수신 중임을 알리는 로그 메시지입니다.
        log.info("listen...");

        // 발급할 쿠폰 요청이 존재하는 동안 반복하여 처리합니다.
        while (existCouponIssueTarget()) {
            // Redis 큐에서 발급 대상 쿠폰 요청을 가져옵니다.
            CouponIssueRequest target = getIssueTarget();

            // 발급을 시작함을 알리는 로그 메시지입니다.
            log.info("발급 시작 target: " + target);

            // 쿠폰 발급 서비스의 issue 메서드를 호출하여 쿠폰을 발급합니다.
            couponIssueService.issue(target.couponId(), target.userId());

            // 발급 완료를 알리는 로그 메시지입니다.
            log.info("발급 완료 target: " + target);

            // 처리된 쿠폰 발급 요청을 큐에서 제거합니다.
            removeIssuedTarget();
        }
    }

    // Redis 큐에 발급할 쿠폰 요청이 존재하는지 확인하는 메서드입니다.
    private boolean existCouponIssueTarget() {
        // Redis 큐의 크기가 0보다 큰지 확인하여 발급 요청이 있는지 판단합니다.
        return redisRepository.lSize(issueRequestQueueKey) > 0;
    }

    // Redis 큐에서 가장 앞에 있는 발급 대상 쿠폰 요청을 가져오는 메서드입니다.
    private CouponIssueRequest getIssueTarget() throws JsonProcessingException {
        // Redis 큐에서 데이터를 가져와 CouponIssueRequest 객체로 변환합니다.
        return objectMapper.readValue(redisRepository.lIndex(issueRequestQueueKey, 0), CouponIssueRequest.class);
    }

    // 발급이 완료된 쿠폰 요청을 Redis 큐에서 제거하는 메서드입니다.
    private void removeIssuedTarget() {
        // Redis 큐에서 가장 앞에 있는 데이터를 제거합니다.
        redisRepository.lPop(issueRequestQueueKey);
    }
}
