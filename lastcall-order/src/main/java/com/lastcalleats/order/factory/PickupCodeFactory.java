package com.lastcalleats.order.factory;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.entity.OrderDO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects the appropriate pickup code generator for a requested output type. The factory keeps
 * generator discovery centralized so service code does not need to know the concrete generator
 * classes.
 */
@Component
public class PickupCodeFactory {

  private final Map<String, PickupCodeGenerator> generators;

  /**
   * Creates the factory from all registered pickup code generators.
   *
   * @param generators generator implementations discovered from the Spring context
   */
  public PickupCodeFactory(List<PickupCodeGenerator> generators) {
    this.generators = generators.stream()
        .collect(Collectors.toMap(PickupCodeGenerator::type, Function.identity()));
  }


  /**
   * Generates a pickup code with the given type.
   *
   * @param order target order
   * @param type  generator type
   * @return generated pickup code
   */
  public String generate(OrderDO order, String type) {
    // Pick the generator by type.
    PickupCodeGenerator generator = generators.get(type);
    if (generator == null) {
      throw new BusinessException(ErrorCode.PICKUP_CODE_INVALID,
          "Unsupported pickup code type: " + type);
    }
    return generator.generate(order);
  }
}
