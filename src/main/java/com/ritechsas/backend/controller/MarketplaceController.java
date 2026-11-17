package com.ritechsas.backend.controller;

import java.util.List;
import static com.ritechsas.backend.dto.MarketplaceDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.ritechsas.backend.service.MarketplaceService;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {
    private final MarketplaceService service;
    public MarketplaceController(MarketplaceService service) { this.service = service; }

    @GetMapping("/catalog") public List<ListingView> catalog() { return service.catalog(); }
    @GetMapping("/products") public List<ProductOption> products() { return service.productOptions(); }

    @GetMapping("/seller/listings") public List<ListingView> myListings(Authentication auth) { return service.myListings(auth.getName()); }
    @PostMapping("/seller/listings") @ResponseStatus(HttpStatus.CREATED) public ListingView createListing(Authentication auth, @RequestBody ListingRequest request) { return service.createListing(auth.getName(), request); }
    @PutMapping("/seller/listings/{id}") public ListingView updateListing(Authentication auth, @PathVariable Long id, @RequestBody ListingRequest request) { return service.updateListing(auth.getName(), id, request); }
    @GetMapping("/seller/lots") public List<LotOption> myLots(Authentication auth) { return service.myLots(auth.getName()); }
    @PostMapping("/seller/lots") @ResponseStatus(HttpStatus.CREATED) public LotOption createLot(Authentication auth, @RequestBody LotRequest request) { return service.createLot(auth.getName(), request); }

    @GetMapping("/cart") public CartView getCart(Authentication auth) { return service.getCart(auth.getName()); }
    @PostMapping("/cart/items") public CartView addCartItem(Authentication auth, @RequestBody CartItemRequest request) { return service.addCartItem(auth.getName(), request); }
    @PutMapping("/cart/items/{id}") public CartView updateCartItem(Authentication auth, @PathVariable Long id, @RequestBody CartQuantityRequest request) { return service.updateCartItem(auth.getName(), id, request); }
    @DeleteMapping("/cart/items/{id}") public CartView removeCartItem(Authentication auth, @PathVariable Long id) { return service.removeCartItem(auth.getName(), id); }
    @PostMapping("/checkout") @ResponseStatus(HttpStatus.CREATED) public OrderView checkout(Authentication auth, @RequestBody(required = false) CheckoutRequest request) { return service.checkout(auth.getName(), request); }
    @GetMapping("/orders") public List<OrderView> myOrders(Authentication auth, @RequestParam(required = false) String role) { return service.myOrders(auth.getName(), role); }
    @PatchMapping("/orders/{id}/status") public OrderView updateOrderStatus(Authentication auth, @PathVariable Long id, @RequestBody OrderStatusRequest request) { return service.updateOrderStatus(auth.getName(), id, request); }
}
