package com.repertorio.hermandad.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.repertorio.hermandad.api.dto.HermandadResponse;
import com.repertorio.hermandad.api.dto.MembersCache;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static java.time.Duration.ofDays;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    public static final String HERMANDAD = "hermandad";
    public static final String HERMANDAD_MEMBER = "hermandad-member";

    private static final String CACHE_PREFIX = "Cache:";

    private final ObjectMapper objectMapper;

    @Bean
    public RedisCacheManagerBuilderCustomizer redisBuilderCustomizer(ObjectMapper objectMapper) {
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(cacheObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                PROPERTY);
        cacheObjectMapper.registerModule(new JavaTimeModule());
        cacheObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer defaultSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        Jackson2JsonRedisSerializer<HermandadResponse> hermandadJackson2JsonRedisSerializer
                = new Jackson2JsonRedisSerializer<>(cacheObjectMapper, HermandadResponse.class);

        Jackson2JsonRedisSerializer<MembersCache> hermandadMemberSerializer
                = new Jackson2JsonRedisSerializer<>(cacheObjectMapper, MembersCache.class);

        return builder -> builder.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith(CACHE_PREFIX)
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer))
                        .entryTtl(ofDays(1)))
                .withCacheConfiguration(HERMANDAD, RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith(CACHE_PREFIX)
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(hermandadJackson2JsonRedisSerializer))
                        .entryTtl(ofDays(2)))
                .withCacheConfiguration(HERMANDAD_MEMBER, RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith(CACHE_PREFIX)
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(hermandadMemberSerializer))
                        .entryTtl(ofDays(2)));
    }

//    @Bean
//    public RedisCacheManager cacheManager(
//            RedisConnectionFactory connectionFactory,
//            ObjectMapper objectMapper
//    ) {
//
//        ObjectMapper redisMapper = objectMapper.copy();
//
//        redisMapper.registerModule(new JavaTimeModule());
//        redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        redisMapper.activateDefaultTyping(
//                BasicPolymorphicTypeValidator.builder()
//                        .allowIfBaseType(Object.class)
//                        .build(),
//                ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS,
//                JsonTypeInfo.As.PROPERTY
//        );
//
//        GenericJackson2JsonRedisSerializer serializer =
//                new GenericJackson2JsonRedisSerializer(redisMapper);
//
//        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
//                .serializeValuesWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
//                );
//
//        return RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(config)
//                .build();
//    }
}
