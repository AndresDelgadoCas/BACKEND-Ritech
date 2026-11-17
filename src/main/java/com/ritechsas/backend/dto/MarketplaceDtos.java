package com.ritechsas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class MarketplaceDtos {
    private MarketplaceDtos() {}

    public record ProductOption(Long id, String name, String productType, String description) {}
    public record ProducerContact(Long id, String name, String contact, String email, String phone) {}
    public record ClientContact(Long id, String companyName, String contactName, String email, String phone, String country, String address) {}
    public record LotOption(Long id, String lotCode, BigDecimal quantity, String status, LocalDate harvestDate, Long productId, String productName) {}
    public record ListingRequest(Long inventoryLotId, String title, String description, BigDecimal priceUsdPerKg, BigDecimal minimumOrderKg, String status) {}
    public record LotRequest(Long productId, String lotCode, BigDecimal quantity, LocalDate harvestDate) {}
    public record ListingView(Long id, String title, String description, BigDecimal priceUsdPerKg, BigDecimal minimumOrderKg, String status, LocalDateTime createdAt, ProductOption product, ProducerContact producer, LotOption inventoryLot) {}
    public record CartItemRequest(Long listingId, BigDecimal quantityKg) {}
    public record CartQuantityRequest(BigDecimal quantityKg) {}
    public record CartItemView(Long id, ListingView listing, BigDecimal quantityKg, BigDecimal lineTotalUsd) {}
    public record CartView(Long id, String status, List<CartItemView> items, BigDecimal totalUsd, String sellerName) {}
    public record CheckoutRequest(String notes) {}
    public record OrderStatusRequest(String status) {}
    public record OrderLineView(Long id, String productName, String productType, String lotCode, BigDecimal quantityKg, BigDecimal priceUsdPerKg, BigDecimal totalUsd) {}
    public record OrderView(Long id, String orderCode, LocalDate orderDate, String status, String destinationCountry, String destinationAddress, String notes, BigDecimal totalUsd, ClientContact buyer, ProducerContact seller, List<OrderLineView> items) {}
    public record ProfileOption(Long id, String label, String secondary, Long linkedUserId) {}
    public record ProfileAssignmentRequest(String profileType, Long profileId) {}
    public record UserStatusRequest(Boolean active) {}
    public record UserRoleRequest(String role) {}
    public record PasswordResetRequest(String password) {}
}
