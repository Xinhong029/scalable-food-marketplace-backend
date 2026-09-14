package com.lastcalleats.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity mapped to the user_favorite table in the database.
 * Store user ID and listing ID to track which listings a user has favorited.
 */
@Entity
@Table(name = "user_favorite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavoriteDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;        // FK to the user table

    @Column(name = "listing_id", nullable = false)
    private Long listingId;     // FK to the product_listing table

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;    // automatically set when the record is inserted
}
