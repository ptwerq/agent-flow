package com.agentflow.clientservice.config;

import com.agentflow.clientservice.dto.response.ClientResponse;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;


@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory) {
        var jsonSerializerResponse = new JacksonJsonRedisSerializer<>(ClientResponse.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, ClientResponse.class);
        var jsonSerializerResponseList = new JacksonJsonRedisSerializer<List<ClientResponse>>(javaType);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
                .disableCachingNullValues()
                .serializeKeysWith(fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(fromSerializer(jsonSerializerResponse));

        Map<String, RedisCacheConfiguration> cacheConfiguration = new HashMap<>();

        cacheConfiguration.put("client", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfiguration.put("clients-lists", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(fromSerializer(jsonSerializerResponseList)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfiguration)
                .transactionAware()
                .build();
    }
}
