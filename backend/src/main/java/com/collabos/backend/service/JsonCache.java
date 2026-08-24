package com.collabos.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;

import java.time.Duration;
import java.util.List;

/**
 * Thin, explicit Redis cache for JSON lists — deliberately not built on
 * Spring's @Cacheable abstraction. That abstraction caches through a
 * type-erased Object, so a cached List<TaskResponse> comes back from Redis
 * as a List<LinkedHashMap> with no way to recover the real element type.
 * Here we pass the target type in at both write and read, so there's never
 * any type information to lose.
 */
@Component
public class JsonCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JsonCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> List<T> getList(String key, Class<T> elementType) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        CollectionType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return objectMapper.readValue(json, type);
    }

    public void putList(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }
}
