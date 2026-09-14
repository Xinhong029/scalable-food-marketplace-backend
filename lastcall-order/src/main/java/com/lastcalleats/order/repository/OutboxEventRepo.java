package com.lastcalleats.order.repository;

import com.lastcalleats.order.entity.OutboxEventDO;
import com.lastcalleats.order.entity.OutboxEventStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Database operations used to persist, claim, and finish transactional outbox events. */
@Repository
public interface OutboxEventRepo extends JpaRepository<OutboxEventDO, Long> {

  long countByStatus(OutboxEventStatus status);

  @Query("""
      select event from OutboxEventDO event
      where event.status = :status
        and event.nextRetryAt <= :now
      order by event.createdAt, event.id
      """)
  List<OutboxEventDO> findReady(
      @Param("status") OutboxEventStatus status,
      @Param("now") LocalDateTime now,
      Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update OutboxEventDO event
      set event.status = :processing,
          event.lockedAt = :lockedAt,
          event.lastError = null
      where event.id = :id
        and event.status = :pending
      """)
  int claim(
      @Param("id") Long id,
      @Param("pending") OutboxEventStatus pending,
      @Param("processing") OutboxEventStatus processing,
      @Param("lockedAt") LocalDateTime lockedAt);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update OutboxEventDO event
      set event.status = :published,
          event.publishedAt = :publishedAt,
          event.lockedAt = null,
          event.lastError = null
      where event.id = :id
        and event.status = :processing
      """)
  int markPublished(
      @Param("id") Long id,
      @Param("processing") OutboxEventStatus processing,
      @Param("published") OutboxEventStatus published,
      @Param("publishedAt") LocalDateTime publishedAt);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update OutboxEventDO event
      set event.status = :pending,
          event.retryCount = event.retryCount + 1,
          event.nextRetryAt = :nextRetryAt,
          event.lockedAt = null,
          event.lastError = :lastError
      where event.id = :id
        and event.status = :processing
      """)
  int reschedule(
      @Param("id") Long id,
      @Param("processing") OutboxEventStatus processing,
      @Param("pending") OutboxEventStatus pending,
      @Param("nextRetryAt") LocalDateTime nextRetryAt,
      @Param("lastError") String lastError);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update OutboxEventDO event
      set event.status = :pending,
          event.nextRetryAt = :now,
          event.lockedAt = null,
          event.lastError = 'Recovered stale publisher claim'
      where event.status = :processing
        and event.lockedAt < :staleBefore
      """)
  int releaseStaleClaims(
      @Param("processing") OutboxEventStatus processing,
      @Param("pending") OutboxEventStatus pending,
      @Param("staleBefore") LocalDateTime staleBefore,
      @Param("now") LocalDateTime now);
}
