package com.lastcalleats.product.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Records low-cardinality metrics for the user-facing product browse path. */
@Component
public class ProductBrowseMetrics {

  public static final String CACHE_HIT = "hit";
  public static final String CACHE_MISS = "miss";
  public static final String CACHE_UNKNOWN = "unknown";
  public static final String OUTCOME_SUCCESS = "success";
  public static final String OUTCOME_FAILURE = "failure";

  private final MeterRegistry registry;

  public ProductBrowseMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  /** Starts a timer sample using the registry's configured clock. */
  public Timer.Sample start() {
    return Timer.start(registry);
  }

  /** Records one completed browse operation and its cache path. */
  public void recordRequest(Timer.Sample sample, String cache, String outcome) {
    Timer timer = Timer.builder("lastcall.product.browse.duration")
        .description("Product browse service latency")
        .tag("cache", cache)
        .tag("outcome", outcome)
        .publishPercentileHistogram()
        .serviceLevelObjectives(
            Duration.ofMillis(1),
            Duration.ofMillis(2),
            Duration.ofMillis(5),
            Duration.ofMillis(10),
            Duration.ofMillis(25),
            Duration.ofMillis(50),
            Duration.ofMillis(100),
            Duration.ofMillis(250),
            Duration.ofMillis(500),
            Duration.ofSeconds(1))
        .register(registry);
    sample.stop(timer);

    Counter.builder("lastcall.product.browse.requests")
        .description("Completed product browse operations")
        .tag("cache", cache)
        .tag("outcome", outcome)
        .register(registry)
        .increment();
  }

  /** Records whether the request was served by Redis or needed the database path. */
  public void recordCacheAccess(String result) {
    Counter.builder("lastcall.product.browse.cache")
        .description("Product browse cache access results")
        .tag("result", result)
        .register(registry)
        .increment();
  }
}
