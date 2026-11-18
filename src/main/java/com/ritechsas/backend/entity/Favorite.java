package com.ritechsas.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name="favorites")
public class Favorite {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="listing_id", nullable=false) private Long listingId;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public Long getListingId(){return listingId;} public void setListingId(Long v){listingId=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
