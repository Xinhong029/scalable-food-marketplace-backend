package com.lastcalleats.order.messaging;

/** Names shared by order message publishers, bindings, and consumers. */
public final class OrderMessagingTopology {

  public static final String ORDER_EXCHANGE = "lastcall.order.exchange";
  public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
  public static final String ORDER_CREATED_QUEUE = "lastcall.order.created.queue";

  public static final String DEAD_LETTER_EXCHANGE = "lastcall.order.dlx";
  public static final String ORDER_CREATED_DEAD_ROUTING_KEY = "order.created.dead";
  public static final String ORDER_CREATED_DEAD_QUEUE = "lastcall.order.created.dlq";

  private OrderMessagingTopology() {
  }
}
