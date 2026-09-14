package com.lastcalleats.order.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents the outcome of a pickup verification attempt. The response includes the verified order
 * context so merchant tools can confirm the correct item is being handed over.
 */
@Getter
@Builder
public class CodeResponse {

  private Long orderId;
  private String customerNickname;
  private String productName;
  private Boolean success;
  private String message;
}
