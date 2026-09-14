package com.lastcalleats.common.response;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * A page of content that reports whether another page exists without running a total-count query.
 * Request and response page numbers are zero-based.
 *
 * @param <T> item type
 */
@Getter
public class SliceResult<T> {

  private final List<T> content;
  private final int page;
  private final int size;
  private final boolean hasNext;

  private SliceResult(List<T> content, int page, int size, boolean hasNext) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.hasNext = hasNext;
  }

  public static <T> SliceResult<T> of(Slice<T> slice) {
    return new SliceResult<>(slice.getContent(), slice.getNumber(), slice.getSize(),
        slice.hasNext());
  }
}
