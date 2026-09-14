package com.lastcalleats.product.cache;

import com.lastcalleats.product.dto.UserBrowseResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

/** JSON-friendly value stored in Redis for one product browse page. */
public record ProductBrowseCacheEntry(
    List<UserBrowseResponse> content,
    int page,
    int size,
    boolean hasNext
) {

  /** Rebuilds the Spring pagination object expected by the service interface. */
  public Slice<UserBrowseResponse> toSlice(Pageable pageable) {
    return new SliceImpl<>(content, pageable, hasNext);
  }
}
