package com.lastcalleats.product.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Cache-Aside access for product browse pages. */
@Component
public class ProductBrowseCache {

  private static final Logger log = LoggerFactory.getLogger(ProductBrowseCache.class);
  private static final String KEY_PREFIX = "lastcall-eats:product-browse:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final Duration ttl;

  public ProductBrowseCache(
      StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.cache.product-browse-ttl:60s}") Duration ttl) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.ttl = ttl;
  }

  /** Returns an empty result on a cache miss or when Redis is temporarily unavailable. */
  public Optional<ProductBrowseCacheEntry> get(int page, int size) {
    String key = key(page, size);
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(json, ProductBrowseCacheEntry.class));
    } catch (Exception exception) {
      log.warn("Product browse cache read failed for key {}; falling back to MySQL", key,
          exception);
      return Optional.empty();
    }
  }

  /** Serializes one page as JSON and stores it with a finite TTL. */
  public void put(ProductBrowseCacheEntry entry) {
    String key = key(entry.page(), entry.size());
    try {
      String json = objectMapper.writeValueAsString(entry);
      redisTemplate.opsForValue().set(key, json, ttl);
    } catch (JsonProcessingException exception) {
      log.warn("Product browse cache serialization failed for key {}", key, exception);
    } catch (RuntimeException exception) {
      log.warn("Product browse cache write failed for key {}; returning database result", key,
          exception);
    }
  }

  /**
   * Deletes all browse pages only after the surrounding database transaction commits.
   * If there is no transaction, invalidation runs immediately.
   */
  public void invalidateAllAfterCommit() {
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          invalidateAll();
        }
      });
      return;
    }
    invalidateAll();
  }

  /** Uses incremental Redis SCAN instead of the blocking KEYS command. */
  public long invalidateAll() {
    ScanOptions options = ScanOptions.scanOptions()
        .match(KEY_PREFIX + "*")
        .count(100)
        .build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      List<String> keys = cursor.stream().toList();
      if (keys.isEmpty()) {
        return 0;
      }
      Long removed = redisTemplate.unlink(keys);
      return removed == null ? 0 : removed;
    } catch (Exception exception) {
      log.warn("Product browse cache invalidation failed; TTL will remove stale entries",
          exception);
      return 0;
    }
  }

  private String key(int page, int size) {
    return KEY_PREFIX + "page:" + page + ":size:" + size;
  }
}
