package com.dustin.couponcore.component;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화합니다.
@RequiredArgsConstructor
// @Component 어노테이션은 이 클래스가 Spring의 관리되는 빈(Bean)임을 나타내며, Spring IoC 컨테이너에서 사용할 수 있게 합니다.
@Component
public class DistributeLockExecutor {

    // Redis 클라이언트로, Redisson 라이브러리를 통해 Redis와 상호작용합니다.
    private final RedissonClient redissonClient;

    // 로깅을 위한 Logger 인스턴스입니다. 이 클래스의 이름을 로깅 메시지에 사용합니다.
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    // 분산 락을 획득하여 지정된 로직을 실행하는 메서드입니다.
    public void execute(String lockName, long waitMilliSecond, long leaseMilliSecond, Runnable logic) {
        // 주어진 lockName을 사용하여 Redis에서 락 객체를 가져옵니다.
        RLock lock = redissonClient.getLock(lockName);
        try {
            // 지정된 시간 동안 락을 획득하려 시도합니다.
            // waitMilliSecond 동안 락을 기다리며, 락을 획득한 후 leaseMilliSecond 동안 락을 유지합니다.
            boolean isLocked = lock.tryLock(waitMilliSecond, leaseMilliSecond, TimeUnit.MILLISECONDS);

            // 락을 획득하지 못한 경우 예외를 발생시킵니다.
            if (!isLocked) {
                throw new IllegalStateException("[" + lockName + "] lock 획득 실패");
            }

            // 락을 성공적으로 획득한 경우, 전달된 로직을 실행합니다.
            logic.run();
        } catch (InterruptedException e) {
            // 락 획득 중 인터럽트가 발생하면 에러 로그를 기록하고 런타임 예외를 발생시킵니다.
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            // 현재 스레드가 락을 보유하고 있는 경우에만 락을 해제합니다.
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
