package com.dustin.couponcore.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

// @RequiredArgsConstructor 어노테이션은 final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 간소화합니다.
@RequiredArgsConstructor
// @Configuration 어노테이션은 이 클래스가 Spring 설정 클래스임을 나타냅니다.
@Configuration
public class CacheConfiguration {

    // Redis와의 연결을 관리하는 RedisConnectionFactory입니다.
    private final RedisConnectionFactory redisConnectionFactory;

    // @Bean 어노테이션은 이 메서드가 Spring 컨텍스트에서 관리되는 빈을 생성함을 나타냅니다.
    // @Primary 어노테이션은 여러 CacheManager 빈이 있을 때 이 빈을 기본적으로 사용하도록 지정합니다.
    @Bean
    @Primary
    public CacheManager redisCacheManager() {
        // RedisCacheConfiguration은 Redis 캐시에 대한 기본 설정을 정의합니다.
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                // 캐시 키를 직렬화할 때 StringRedisSerializer를 사용하여 문자열로 변환합니다.
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 캐시 값을 직렬화할 때 GenericJackson2JsonRedisSerializer를 사용하여 JSON으로 변환합니다.
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 캐시 항목의 TTL(Time to Live)을 30분으로 설정합니다.
                .entryTtl(Duration.ofMinutes(30));

        // RedisCacheManager를 생성하여 RedisConnectionFactory와 기본 캐시 설정을 사용하여 관리합니다.
        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory)
                // 기본 캐시 설정을 적용합니다.
                .cacheDefaults(redisCacheConfiguration)
                // RedisCacheManager 객체를 빌드하여 반환합니다.
                .build();
    }
}
