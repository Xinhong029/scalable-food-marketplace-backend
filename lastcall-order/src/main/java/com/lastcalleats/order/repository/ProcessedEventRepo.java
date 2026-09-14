package com.lastcalleats.order.repository;

import com.lastcalleats.order.entity.ProcessedEventDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Atomically records a consumer's first attempt to apply an event. */
@Repository
public interface ProcessedEventRepo extends JpaRepository<ProcessedEventDO, Long> {

  @Modifying(flushAutomatically = true)
  @Query(value = """
      INSERT IGNORE INTO processed_event (event_id, consumer_name, processed_at)
      VALUES (:eventId, :consumerName, NOW(6))
      """, nativeQuery = true)
  int recordIfAbsent(
      @Param("eventId") String eventId,
      @Param("consumerName") String consumerName);

  long countByEventIdAndConsumerName(String eventId, String consumerName);
}
