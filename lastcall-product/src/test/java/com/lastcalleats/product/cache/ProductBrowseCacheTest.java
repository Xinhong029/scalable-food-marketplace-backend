package com.lastcalleats.product.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Tests transaction timing and Redis key removal for browse cache invalidation. */
@ExtendWith(MockitoExtension.class)
class ProductBrowseCacheTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private Cursor<String> cursor;

  @AfterEach
  void clearTransactionState() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
    TransactionSynchronizationManager.setActualTransactionActive(false);
  }

  @Test
  void invalidateAllAfterCommit_waitsForDatabaseCommit() {
    ProductBrowseCache cache = cache();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();
    when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    when(cursor.stream()).thenReturn(Stream.of(
        "lastcall-eats:product-browse:page:0:size:20",
        "lastcall-eats:product-browse:page:1:size:20"));
    when(redisTemplate.unlink(anyCollection())).thenReturn(2L);

    cache.invalidateAllAfterCommit();

    verify(redisTemplate, never()).scan(any(ScanOptions.class));
    for (TransactionSynchronization synchronization
        : TransactionSynchronizationManager.getSynchronizations()) {
      synchronization.afterCommit();
    }
    verify(redisTemplate).scan(any(ScanOptions.class));
    verify(redisTemplate).unlink(anyCollection());
  }

  @Test
  void invalidateAll_withoutTransaction_runsImmediately() {
    ProductBrowseCache cache = cache();
    when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    when(cursor.stream()).thenReturn(Stream.empty());

    long removed = cache.invalidateAll();

    assertEquals(0, removed);
    verify(redisTemplate).scan(any(ScanOptions.class));
    verify(redisTemplate, never()).unlink(anyCollection());
  }

  private ProductBrowseCache cache() {
    return new ProductBrowseCache(redisTemplate, new ObjectMapper(), Duration.ofSeconds(60));
  }
}
