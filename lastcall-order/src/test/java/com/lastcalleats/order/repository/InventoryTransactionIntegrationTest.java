package com.lastcalleats.order.repository;

import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.OutboxEventDO;
import com.lastcalleats.order.entity.OutboxEventStatus;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies inventory and order persistence share one rollback boundary. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InventoryTransactionIntegrationTest {

  @Configuration
  @EnableAutoConfiguration
  @EntityScan(basePackages = {
      "com.lastcalleats.order.entity",
      "com.lastcalleats.product.entity",
      "com.lastcalleats.merchant.entity"
  })
  @EnableJpaRepositories(basePackages = {
      "com.lastcalleats.order.repository", "com.lastcalleats.product.repository"})
  static class TestConfig {}

  @Autowired
  private ProductListingRepo productListingRepo;

  @Autowired
  private OrderRepo orderRepo;

  @Autowired
  private OutboxEventRepo outboxEventRepo;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @BeforeEach
  void clearTransactionalRecords() {
    requiredTransaction().execute(status -> {
      outboxEventRepo.deleteAll();
      orderRepo.deleteAll();
      return null;
    });
  }

  @Test
  void failedOrderInsertRollsBackInventoryReservation() {
    ProductListingDO listing = requiredTransaction().execute(status -> productListingRepo.save(
        ProductListingDO.builder()
            .merchantId(20L)
            .templateId(30L)
            .discountPrice(new BigDecimal("6.99"))
            .quantity(1)
            .remainingQuantity(1)
            .pickupStart(LocalTime.of(18, 0))
            .pickupEnd(LocalTime.of(20, 0))
            .date(LocalDate.now())
            .isAvailable(true)
            .build()));

    assertThrows(DataIntegrityViolationException.class, () -> requiredTransaction().execute(status -> {
      assertEquals(1, productListingRepo.reserveOneIfAvailable(listing.getId()));
      // Null userId violates the order table constraint when saveAndFlush executes.
      orderRepo.saveAndFlush(OrderDO.builder()
          .listingId(listing.getId())
          .merchantId(listing.getMerchantId())
          .price(listing.getDiscountPrice())
          .status(OrderDO.OrderStatus.PENDING_PAYMENT)
          .build());
      return null;
    }));

    ProductListingDO afterRollback = requiredTransaction().execute(status ->
        productListingRepo.findById(listing.getId()).orElseThrow());
    assertEquals(1, afterRollback.getRemainingQuantity());
    assertEquals(true, afterRollback.getIsAvailable());
    assertEquals(0, orderRepo.count());
  }

  @Test
  void failedOutboxInsertRollsBackOrderAndInventoryReservation() {
    ProductListingDO listing = requiredTransaction().execute(status -> productListingRepo.save(
        ProductListingDO.builder()
            .merchantId(20L)
            .templateId(30L)
            .discountPrice(new BigDecimal("7.99"))
            .quantity(1)
            .remainingQuantity(1)
            .pickupStart(LocalTime.of(18, 0))
            .pickupEnd(LocalTime.of(20, 0))
            .date(LocalDate.now())
            .isAvailable(true)
            .build()));

    assertThrows(DataIntegrityViolationException.class, () -> requiredTransaction().execute(status -> {
      assertEquals(1, productListingRepo.reserveOneIfAvailable(listing.getId()));
      OrderDO order = orderRepo.saveAndFlush(OrderDO.builder()
          .userId(100L)
          .listingId(listing.getId())
          .merchantId(listing.getMerchantId())
          .price(listing.getDiscountPrice())
          .status(OrderDO.OrderStatus.PENDING_PAYMENT)
          .build());
      // Missing eventId violates the outbox constraint and must roll back the earlier writes.
      outboxEventRepo.saveAndFlush(OutboxEventDO.builder()
          .aggregateType("ORDER")
          .aggregateId(order.getId())
          .eventType("ORDER_CREATED")
          .exchangeName("lastcall.order.exchange")
          .routingKey("order.created")
          .payload("{}")
          .status(OutboxEventStatus.PENDING)
          .retryCount(0)
          .nextRetryAt(LocalDateTime.now())
          .build());
      return null;
    }));

    ProductListingDO afterRollback = requiredTransaction().execute(status ->
        productListingRepo.findById(listing.getId()).orElseThrow());
    assertEquals(1, afterRollback.getRemainingQuantity());
    assertEquals(true, afterRollback.getIsAvailable());
    assertEquals(0, orderRepo.count());
    assertEquals(0, outboxEventRepo.count());
  }

  @Test
  void conditionalClaim_allowsOnlyTheFirstPublisherToWin() {
    OutboxEventDO event = requiredTransaction().execute(status -> outboxEventRepo.save(
        OutboxEventDO.builder()
            .eventId("11111111-1111-1111-1111-111111111111")
            .aggregateType("ORDER")
            .aggregateId(900L)
            .eventType("ORDER_CREATED")
            .exchangeName("lastcall.order.exchange")
            .routingKey("order.created")
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .nextRetryAt(LocalDateTime.now())
            .build()));

    Integer firstClaim = requiredTransaction().execute(status -> outboxEventRepo.claim(
        event.getId(), OutboxEventStatus.PENDING, OutboxEventStatus.PROCESSING,
        LocalDateTime.now()));
    Integer secondClaim = requiredTransaction().execute(status -> outboxEventRepo.claim(
        event.getId(), OutboxEventStatus.PENDING, OutboxEventStatus.PROCESSING,
        LocalDateTime.now()));

    assertEquals(1, firstClaim);
    assertEquals(0, secondClaim);
  }

  private TransactionTemplate requiredTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transaction;
  }
}
