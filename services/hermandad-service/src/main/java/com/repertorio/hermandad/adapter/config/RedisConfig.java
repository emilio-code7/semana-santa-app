package com.repertorio.hermandad.adapter.config;

import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.repertorio.hermandad.adapter.inbound.rest.dto.HermandadResponse;
import com.repertorio.hermandad.adapter.inbound.rest.dto.MembersCache;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

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
        ObjectMapper cacheObjectMapper = objectMapper.rebuild()
                .activateDefaultTypingAsProperty(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build(),
                        DefaultTyping.NON_FINAL,
                        "@class"
                )
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        GenericJacksonJsonRedisSerializer defaultSerializer = new GenericJacksonJsonRedisSerializer(cacheObjectMapper);

        JacksonJsonRedisSerializer<HermandadResponse> hermandadJacksonJsonRedisSerializer
                = new JacksonJsonRedisSerializer<>(cacheObjectMapper, HermandadResponse.class);

        JacksonJsonRedisSerializer<MembersCache> hermandadMemberSerializer
                = new JacksonJsonRedisSerializer<>(cacheObjectMapper, MembersCache.class);

        return builder -> builder.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith(CACHE_PREFIX)
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer))
                        .entryTtl(ofDays(1)))
                .withCacheConfiguration(HERMANDAD, RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith(CACHE_PREFIX)
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(hermandadJacksonJsonRedisSerializer))
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
