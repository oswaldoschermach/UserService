package com.nebula.userService.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Serviço de rate limiting por IP usando Bucket4j + Redis.
 * Cada IP recebe um bucket independente, persistido no Redis.
 */
@Slf4j
@Service
public class RateLimitService {

    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfiguration;

    public RateLimitService(
            RedisClient redisClient,
            @Value("${rate-limit.create-user.capacity:10}") long capacity,
            @Value("${rate-limit.create-user.refill-tokens:10}") long refillTokens,
            @Value("${rate-limit.create-user.refill-seconds:60}") long refillSeconds) {

        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        this.proxyManager = LettuceBasedProxyManager.builderFor(connection).build();

        this.bucketConfiguration = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(refillTokens, Duration.ofSeconds(refillSeconds))
                        .build())
                .build();
    }

    /**
     * Tenta consumir 1 token do bucket do IP informado.
     *
     * @param clientIp endereço IP do cliente
     * @return true se a requisição é permitida, false se o limite foi atingido
     */
    public boolean tryConsume(String clientIp) {
        String key = "rate_limit:create_user:" + clientIp;
        return proxyManager.builder()
                .build(key, bucketConfiguration)
                .tryConsume(1);
    }
}
