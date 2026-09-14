package com.lastcalleats.product.service;

import com.lastcalleats.product.dto.ListingRequest;
import com.lastcalleats.product.dto.ListingResponse;
import com.lastcalleats.product.dto.UserBrowseResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * Service interface for product listing related business logic.
 *
 * A listing is the actual daily item a merchant publishes for sale.
 */
public interface ProductListingService {

  ListingResponse createListing(Long merchantId, ListingRequest request);

  List<ListingResponse> getMerchantListings(Long merchantId);

  void deactivateListing(Long merchantId, Long listingId);

  Slice<UserBrowseResponse> browseAvailableListings(Pageable pageable);
}
