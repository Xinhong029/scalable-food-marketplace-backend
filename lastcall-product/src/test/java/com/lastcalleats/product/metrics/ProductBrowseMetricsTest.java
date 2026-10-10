package com.lastcalleats.product.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ProductBrowseMetricsTest {

  @Test
  void recordsLatencyRequestOutcomeAndCacheAccessWithBoundedTags() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ProductBrowseMetrics metrics = new ProductBrowseMetrics(registry);

    Timer.Sample sample = metrics.start();
    metrics.recordCacheAccess(ProductBrowseMetrics.CACHE_HIT);
    metrics.recordRequest(
        sample, ProductBrowseMetrics.CACHE_HIT, ProductBrowseMetrics.OUTCOME_SUCCESS);

    Timer timer = registry.find("lastcall.product.browse.duration")
        .tags("cache", "hit", "outcome", "success")
        .timer();
    assertNotNull(timer);
    assertEquals(1, timer.count());
    assertEquals(1.0, registry.find("lastcall.product.browse.requests")
        .tags("cache", "hit", "outcome", "success")
        .counter()
        .count());
    assertEquals(1.0, registry.find("lastcall.product.browse.cache")
        .tag("result", "hit")
        .counter()
        .count());
  }
}
