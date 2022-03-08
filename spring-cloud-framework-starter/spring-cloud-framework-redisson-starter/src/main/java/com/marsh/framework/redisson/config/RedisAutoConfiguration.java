package com.marsh.framework.redisson.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 初始化redis的key和value序列化方式
 * @author Marsh
 * @date 2021/8/30
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({RedisProperties.class})
@ComponentScan(basePackages = {"com.marsh.framework.redisson"})
public class RedisAutoConfiguration {

    /**
     * 指定key序列化使用string字符串
     * @author Marsh
     * @date 2021/8/30
     * @return org.springframework.data.redis.serializer.RedisSerializer<java.lang.String>
     */
    @Bean
    public RedisSerializer<String> redisKeySerializer() {
        return RedisSerializer.string();
    }

    /**
     * 指定value的序列化使用json
     * @author Marsh
     * @date 2021/8/30
     * @return org.springframework.data.redis.serializer.RedisSerializer<java.lang.Object>
     */
    @Bean
    public RedisSerializer<Object> redisValueSerializer() {
        return RedisSerializer.json();
    }

    /**
     * RedisTemplate配置
     *
     * @param factory
     * @param redisKeySerializer
     * @param redisValueSerializer
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, RedisSerializer<String> redisKeySerializer, RedisSerializer<Object> redisValueSerializer) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setDefaultSerializer(redisValueSerializer);
        redisTemplate.setKeySerializer(redisKeySerializer);
        redisTemplate.setHashKeySerializer(redisKeySerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
