package com.ritechsas.backend.service;

import static com.ritechsas.backend.dto.MarketplaceDtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.ritechsas.backend.entity.*;
import com.ritechsas.backend.repository.*;

@Service
public class MarketplaceService {
    private final UserRepository users;
    private final ProducerRepository producers;
    private final ClientRepository clients;
    private final ProductRepository products;
    private final InventoryLotRepository lots;
    private final ProductListingRepository listings;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ExportOrderRepository orders;
    private final ExportOrderItemRepository orderItems;
    private final AuditLogRepository audit;

    public MarketplaceService(UserRepository users, ProducerRepository producers, ClientRepository clients, ProductRepository products, InventoryLotRepository lots, ProductListingRepository listings, CartRepository carts, CartItemRepository cartItems, ExportOrderRepository orders, ExportOrderItemRepository orderItems, AuditLogRepository audit) {
        this.users = users; this.producers = producers; this.clients = clients; this.products = products; this.lots = lots; this.listings = listings; this.carts = carts; this.cartItems = cartItems; this.orders = orders; this.orderItems = orderItems; this.audit = audit;
    }

    private User user(String username) { return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")); }
    private void requireRole(User user, String role) { if (!user.hasRole(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para esta operación"); }
    private Producer producerProfile(User user) {
        return producers.findByUserId(user.getId()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.CONFLICT, "Tu cuenta todavía no tiene un perfil de productor vinculado. Contacta al administrador."));
    }

    private Producer activeProducer(User user) {
        Producer producer = producerProfile(user);
        if (!"ACTIVE".equalsIgnoreCase(producer.getSellerStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu perfil de vendedor está suspendido. Puedes consultar tu historial, pero no realizar operaciones comerciales.");
        }
        return producer;
    }

    private void requireActiveListingSeller(ProductListing listing) {
        if (!"ACTIVE".equalsIgnoreCase(listing.getProducer().getSellerStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El vendedor de esta publicación está suspendido. Elimina el artículo del carrito o contacta a soporte.");
        }
    }
    private Client client(User user) { return clients.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Tu cuenta todavía no tiene un perfil de comprador vinculado. Contacta al administrador.")); }
    private <T> T notFound(Optional<T> value, String message) { return value.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message)); }
    private void log(User actor, String action, String entity, Long id, Map<String,Object> details) { AuditLog a = new AuditLog(); a.setActorUserId(actor == null ? null : actor.getId()); a.setAction(action); a.setEntityType(entity); a.setEntityId(id); a.setDetails(details); audit.save(a); }

    @Transactional(readOnly = true)
    public List<ProductOption> productOptions() { return products.findAll().stream().map(p -> new ProductOption(p.getId(), p.getName(), p.getProductType(), p.getDescription())).toList(); }

    @Transactional(readOnly = true)
    public List<ListingView> catalog() {
        return listings.findByStatusOrderByCreatedAtDesc(ProductListing.ListingStatus.PUBLISHED).stream()
            .filter(l -> "ACTIVE".equalsIgnoreCase(l.getProducer().getSellerStatus()))
            .filter(l -> l.getInventoryLot().getStatus() == InventoryLot.LotStatus.AVAILABLE && l.getInventoryLot().getQuantity().compareTo(l.getMinimumOrderKg()) >= 0)
            .map(this::listingView).toList();
    }

    @Transactional(readOnly = true)
    public List<ListingView> myListings(String username) { User u = user(username); requireRole(u, "VENDEDOR"); producerProfile(u); return listings.findByProducer_UserIdOrderByCreatedAtDesc(u.getId()).stream().map(this::listingView).toList(); }

    @Transactional(readOnly = true)
    public List<LotOption> myLots(String username) { User u = user(username); requireRole(u, "VENDEDOR"); Producer p = producerProfile(u); return lots.findByProducer_Id(p.getId()).stream().map(this::lotOption).toList(); }

    @Transactional
    public LotOption createLot(String username, LotRequest request) {
        User u = user(username); requireRole(u, "VENDEDOR"); Producer p = activeProducer(u);
        if (request.productId() == null || request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0 || request.lotCode() == null || request.lotCode().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa el producto, código y cantidad del lote");
        if (request.lotCode().trim().length() > 80) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código del lote no puede superar 80 caracteres");
        if (lots.existsByLotCode(request.lotCode().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "El código de lote ya existe");
        BigDecimal normalizedQuantity = request.quantity().setScale(3, RoundingMode.HALF_UP);
        if (normalizedQuantity.compareTo(BigDecimal.ZERO) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad mínima es 0,001 kg");
        Product product = notFound(products.findById(request.productId()), "Producto no encontrado");
        String producerType = p.getProductType() == null ? "" : p.getProductType().toUpperCase(Locale.ROOT);
        String productType = product.getProductType() == null ? "" : product.getProductType().toUpperCase(Locale.ROOT);
        if (!"BOTH".equals(producerType) && !producerType.equals(productType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no corresponde al tipo de producción de tu perfil");
        }
        InventoryLot lot = new InventoryLot(); lot.setProducer(p); lot.setProduct(product); lot.setLotCode(request.lotCode().trim()); lot.setQuantity(normalizedQuantity); lot.setHarvestDate(request.harvestDate()); lot.setStatus(InventoryLot.LotStatus.AVAILABLE);
        lot = lots.save(lot); log(u, "LOT_CREATED", "inventory_lots", lot.getId(), Map.of("lotCode", lot.getLotCode())); return lotOption(lot);
    }

    @Transactional
    public ListingView createListing(String username, ListingRequest request) {
        User u = user(username); requireRole(u, "VENDEDOR"); Producer p = activeProducer(u);
        if (request.inventoryLotId() == null || request.title() == null || request.title().isBlank() || request.priceUsdPerKg() == null || request.priceUsdPerKg().compareTo(BigDecimal.ZERO) <= 0 || request.minimumOrderKg() == null || request.minimumOrderKg().compareTo(BigDecimal.ZERO) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa el título, el lote, el precio y el pedido mínimo");
        if (request.title().trim().length() > 180) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título no puede superar 180 caracteres");
        BigDecimal normalizedPrice = request.priceUsdPerKg().setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedMinimum = request.minimumOrderKg().setScale(2, RoundingMode.HALF_UP);
        if (normalizedPrice.compareTo(BigDecimal.ZERO) <= 0 || normalizedMinimum.compareTo(BigDecimal.ZERO) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio y el pedido mínimo deben ser mayores que cero con dos decimales");
        InventoryLot lot = notFound(lots.findByIdForUpdate(request.inventoryLotId()), "Lote no encontrado");
        if (!Objects.equals(lot.getProducer().getId(), p.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes publicar inventario de tu propio perfil");
        if (lot.getStatus() != InventoryLot.LotStatus.AVAILABLE || lot.getQuantity().compareTo(normalizedMinimum) < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "El lote no tiene disponibilidad suficiente");
        if (listings.existsByInventoryLot_Id(lot.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este lote ya tiene una publicación asociada");
        ProductListing listing = new ProductListing(); listing.setProducer(p); listing.setProduct(lot.getProduct()); listing.setInventoryLot(lot); listing.setTitle(request.title().trim()); listing.setDescription(request.description()); listing.setPriceUsdPerKg(normalizedPrice); listing.setMinimumOrderKg(normalizedMinimum);
        listing.setStatus(parseListingStatus(request.status())); listing = listings.save(listing); log(u, "LISTING_CREATED", "product_listings", listing.getId(), Map.of("title", listing.getTitle())); return listingView(listing);
    }

    @Transactional
    public ListingView updateListing(String username, Long id, ListingRequest request) {
        User u = user(username); requireRole(u, "VENDEDOR"); activeProducer(u); ProductListing listing = notFound(listings.findByIdAndProducer_UserId(id, u.getId()), "Publicación no encontrada");
        InventoryLot lockedLot = notFound(lots.findByIdForUpdate(listing.getInventoryLot().getId()), "Lote no encontrado");
        listing.setInventoryLot(lockedLot);
        if (request.title() != null && !request.title().isBlank()) {
            if (request.title().trim().length() > 180) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título no puede superar 180 caracteres");
            listing.setTitle(request.title().trim());
        }
        if (request.description() != null) listing.setDescription(request.description());
        if (request.priceUsdPerKg() != null) {
            BigDecimal normalizedPrice = request.priceUsdPerKg().setScale(2, RoundingMode.HALF_UP);
            if (normalizedPrice.compareTo(BigDecimal.ZERO) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio debe ser mayor que cero con dos decimales");
            listing.setPriceUsdPerKg(normalizedPrice);
        }
        if (request.minimumOrderKg() != null) {
            BigDecimal normalizedMinimum = request.minimumOrderKg().setScale(2, RoundingMode.HALF_UP);
            if (normalizedMinimum.compareTo(BigDecimal.ZERO) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido mínimo debe ser mayor que cero con dos decimales");
            listing.setMinimumOrderKg(normalizedMinimum);
        }
        if (request.status() != null) {
            ProductListing.ListingStatus requestedStatus = parseListingStatus(request.status());
            if (requestedStatus == ProductListing.ListingStatus.PUBLISHED && (listing.getInventoryLot().getStatus() != InventoryLot.LotStatus.AVAILABLE || listing.getInventoryLot().getQuantity().compareTo(listing.getMinimumOrderKg()) < 0)) throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes publicar un lote sin stock suficiente");
            if (requestedStatus == ProductListing.ListingStatus.SOLD_OUT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado Agotado se asigna automáticamente según el inventario");
            listing.setStatus(requestedStatus);
        }
        if (listing.getStatus() == ProductListing.ListingStatus.PUBLISHED && (listing.getInventoryLot().getStatus() != InventoryLot.LotStatus.AVAILABLE || listing.getInventoryLot().getQuantity().compareTo(listing.getMinimumOrderKg()) < 0)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido mínimo supera el inventario disponible");
        listing = listings.save(listing); log(u, "LISTING_UPDATED", "product_listings", id, Map.of("status", listing.getStatus().name())); return listingView(listing);
    }

    private ProductListing.ListingStatus parseListingStatus(String value) {
        if (value == null || value.isBlank()) return ProductListing.ListingStatus.PUBLISHED;
        try {
            ProductListing.ListingStatus status = ProductListing.ListingStatus.valueOf(value.toUpperCase(Locale.ROOT));
            if (status == ProductListing.ListingStatus.SOLD_OUT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado Agotado se asigna automáticamente según el inventario");
            return status;
        } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de publicación inválido"); }
    }

    @Transactional
    public CartView getCart(String username) { User u = user(username); requireRole(u, "COMPRADOR"); client(u); return cartView(activeCart(u)); }

    private Cart activeCart(User u) { return carts.findFirstByBuyerUserIdAndStatus(u.getId(), Cart.CartStatus.ACTIVE).orElseGet(() -> { Cart c = new Cart(); c.setBuyerUserId(u.getId()); c.setStatus(Cart.CartStatus.ACTIVE); return carts.save(c); }); }

    @Transactional
    public CartView addCartItem(String username, CartItemRequest request) {
        User u = user(username); requireRole(u, "COMPRADOR"); client(u);
        if (request.listingId() == null || request.quantityKg() == null || request.quantityKg().compareTo(BigDecimal.ZERO) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad inválida");
        Cart cart = activeCart(u); ProductListing listing = notFound(listings.findById(request.listingId()), "Publicación no encontrada");
        requireActiveListingSeller(listing);
        if (listing.getStatus() != ProductListing.ListingStatus.PUBLISHED || listing.getInventoryLot().getStatus() != InventoryLot.LotStatus.AVAILABLE) throw new ResponseStatusException(HttpStatus.CONFLICT, "La publicación ya no está disponible");
        List<CartItem> current = cartItems.findByCart_Id(cart.getId());
        if (!current.isEmpty() && !Objects.equals(current.get(0).getListing().getProducer().getId(), listing.getProducer().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "El carrito solo puede contener productos de un vendedor a la vez");
        CartItem item = current.stream().filter(i -> Objects.equals(i.getListing().getId(), listing.getId())).findFirst().orElseGet(() -> { CartItem n = new CartItem(); n.setCart(cart); n.setListing(listing); n.setQuantityKg(BigDecimal.ZERO); return n; });
        BigDecimal increment = request.quantityKg().setScale(3, RoundingMode.HALF_UP);
        if (increment.compareTo(BigDecimal.ZERO) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad mínima es 0,001 kg");
        BigDecimal qty = item.getQuantityKg().add(increment).setScale(3, RoundingMode.HALF_UP);
        BigDecimal requestedForLot = current.stream()
            .filter(existing -> Objects.equals(existing.getListing().getInventoryLot().getId(), listing.getInventoryLot().getId()))
            .map(CartItem::getQuantityKg).reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(increment).setScale(3, RoundingMode.HALF_UP);
        if (qty.compareTo(listing.getMinimumOrderKg()) < 0 || requestedForLot.compareTo(listing.getInventoryLot().getQuantity()) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La cantidad debe cumplir el mínimo y no superar el stock disponible");
        item.setQuantityKg(qty); cartItems.save(item); log(u, "CART_ITEM_ADDED", "cart_items", item.getId(), Map.of("listingId", listing.getId(), "quantityKg", qty)); return cartView(cart);
    }

    @Transactional
    public CartView updateCartItem(String username, Long itemId, CartQuantityRequest request) {
        User u = user(username); requireRole(u, "COMPRADOR"); CartItem item = notFound(cartItems.findByIdAndCart_BuyerUserId(itemId, u.getId()), "Artículo del carrito no encontrado");
        if (request.quantityKg() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad es obligatoria");
        BigDecimal normalizedQuantity = request.quantityKg().setScale(3, RoundingMode.HALF_UP);
        requireActiveListingSeller(item.getListing());
        if (item.getListing().getStatus() != ProductListing.ListingStatus.PUBLISHED || item.getListing().getInventoryLot().getStatus() != InventoryLot.LotStatus.AVAILABLE) throw new ResponseStatusException(HttpStatus.CONFLICT, "La publicación ya no está disponible. Elimina el artículo del carrito.");
        if (normalizedQuantity.compareTo(item.getListing().getMinimumOrderKg()) < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad es inferior al pedido mínimo");
        BigDecimal otherQuantityForLot = cartItems.findByCart_Id(item.getCart().getId()).stream()
            .filter(existing -> !Objects.equals(existing.getId(), item.getId()) && Objects.equals(existing.getListing().getInventoryLot().getId(), item.getListing().getInventoryLot().getId()))
            .map(CartItem::getQuantityKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (otherQuantityForLot.add(normalizedQuantity).compareTo(item.getListing().getInventoryLot().getQuantity()) > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad supera el stock disponible");
        item.setQuantityKg(normalizedQuantity); cartItems.save(item); return cartView(item.getCart());
    }

    @Transactional
    public CartView removeCartItem(String username, Long itemId) {
        User u = user(username); requireRole(u, "COMPRADOR"); CartItem item = notFound(cartItems.findByIdAndCart_BuyerUserId(itemId, u.getId()), "Artículo del carrito no encontrado"); Cart cart = item.getCart(); cartItems.delete(item); return cartView(cart);
    }

    @Transactional
    public OrderView checkout(String username, CheckoutRequest request) {
        User u = user(username); requireRole(u, "COMPRADOR"); Client buyer = client(u); Cart cart = activeCart(u); List<CartItem> items = cartItems.findByCart_Id(cart.getId());
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tu carrito está vacío");
        Long sellerId = items.get(0).getListing().getProducer().getId();
        if (items.stream().anyMatch(i -> !Objects.equals(i.getListing().getProducer().getId(), sellerId))) throw new ResponseStatusException(HttpStatus.CONFLICT, "El carrito contiene más de un vendedor");
        Map<Long, InventoryLot> lockedLots = new LinkedHashMap<>();
        items.stream().map(item -> item.getListing().getInventoryLot().getId()).distinct().sorted().forEach(lotId ->
            lockedLots.put(lotId, notFound(lots.findByIdForUpdate(lotId), "Lote de inventario no encontrado"))
        );
        Map<Long, BigDecimal> quantityByLot = new HashMap<>();
        for (CartItem item : items) {
            ProductListing listing = item.getListing();
            InventoryLot lot = lockedLots.get(listing.getInventoryLot().getId());
            requireActiveListingSeller(listing);
            if (listing.getStatus() != ProductListing.ListingStatus.PUBLISHED || lot.getStatus() != InventoryLot.LotStatus.AVAILABLE || item.getQuantityKg().compareTo(listing.getMinimumOrderKg()) < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "El stock o la publicación cambió. Actualiza tu carrito e inténtalo de nuevo.");
            quantityByLot.merge(lot.getId(), item.getQuantityKg(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> entry : quantityByLot.entrySet()) {
            if (entry.getValue().compareTo(lockedLots.get(entry.getKey()).getQuantity()) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido supera el inventario disponible. Actualiza tu carrito e inténtalo de nuevo.");
        }
        ExportOrder order = new ExportOrder(); order.setOrderCode("RT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)); order.setClient(buyer); order.setDestinationCountry(buyer.getCountry()); order.setDestinationAddress(buyer.getAddress()); order.setOrderDate(LocalDate.now()); order.setStatus(ExportOrder.OrderStatus.CONFIRMED); order.setNotes(request == null ? null : request.notes()); order = orders.save(order);
        for (CartItem cartItem : items) {
            ProductListing listing = cartItem.getListing();
            InventoryLot lot = lockedLots.get(listing.getInventoryLot().getId());
            ExportOrderItem line = new ExportOrderItem(); line.setExportOrder(order); line.setProduct(listing.getProduct()); line.setInventoryLot(lot); line.setQuantityKg(cartItem.getQuantityKg()); line.setSalePriceUsdPerKg(listing.getPriceUsdPerKg().setScale(4, RoundingMode.HALF_UP)); orderItems.save(line);
            lot.setQuantity(lot.getQuantity().subtract(cartItem.getQuantityKg()));
            if (lot.getQuantity().compareTo(BigDecimal.ZERO) <= 0) { lot.setQuantity(BigDecimal.ZERO); lot.setStatus(InventoryLot.LotStatus.EXPORTED); }
            else { lot.setStatus(InventoryLot.LotStatus.AVAILABLE); }
            lots.save(lot);
            for (ProductListing related : listings.findByInventoryLot_Id(lot.getId())) {
                if (lot.getQuantity().compareTo(related.getMinimumOrderKg()) < 0 && related.getStatus() == ProductListing.ListingStatus.PUBLISHED) related.setStatus(ProductListing.ListingStatus.SOLD_OUT);
                listings.save(related);
            }
        }
        cart.setStatus(Cart.CartStatus.CONVERTED); carts.save(cart); log(u, "ORDER_CREATED", "export_orders", order.getId(), Map.of("orderCode", order.getOrderCode(), "sellerId", sellerId)); return orderView(order);
    }

    @Transactional(readOnly = true)
    public List<OrderView> myOrders(String username) { return myOrders(username, null); }

    @Transactional(readOnly = true)
    public List<OrderView> myOrders(String username, String requestedRole) {
        User u = user(username); List<ExportOrder> found;
        String viewRole = requestedRole == null || requestedRole.isBlank() ? u.getRole().getName() : requestedRole.trim().toUpperCase(Locale.ROOT);
        if ("COMPRADOR".equals(viewRole) && u.hasRole("COMPRADOR")) { Client c = client(u); found = orders.findByClient_UserIdOrderByOrderDateDesc(u.getId()); }
        else if ("VENDEDOR".equals(viewRole) && u.hasRole("VENDEDOR")) { producerProfile(u); LinkedHashMap<Long,ExportOrder> unique = new LinkedHashMap<>(); for (ExportOrderItem item : orderItems.findByInventoryLot_Producer_UserId(u.getId())) unique.put(item.getExportOrder().getId(), item.getExportOrder()); found = unique.values().stream().sorted(Comparator.comparing(ExportOrder::getOrderDate).reversed()).toList(); }
        else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El administrador no tiene acceso a pedidos comerciales");
        return found.stream().map(this::orderView).toList();
    }

    @Transactional
    public OrderView updateOrderStatus(String username, Long orderId, OrderStatusRequest request) {
        User u = user(username); requireRole(u, "VENDEDOR"); activeProducer(u); ExportOrder order = notFound(orders.findById(orderId), "Pedido no encontrado");
        List<ExportOrderItem> lines = orderItems.findByExportOrder_Id(orderId);
        if (lines.isEmpty() || lines.stream().anyMatch(i -> !Objects.equals(i.getInventoryLot().getProducer().getUserId(), u.getId()))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes gestionar pedidos de tus propios productos");
        ExportOrder.OrderStatus next; try { next = ExportOrder.OrderStatus.valueOf(request.status().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pedido inválido"); }
        ExportOrder.OrderStatus current = order.getStatus();
        boolean allowed = (current == ExportOrder.OrderStatus.CONFIRMED && (next == ExportOrder.OrderStatus.IN_PREPARATION || next == ExportOrder.OrderStatus.CANCELLED)) || (current == ExportOrder.OrderStatus.IN_PREPARATION && next == ExportOrder.OrderStatus.SHIPPED) || (current == ExportOrder.OrderStatus.SHIPPED && next == ExportOrder.OrderStatus.DELIVERED);
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "No está permitido cambiar de " + current + " a " + next);
        if (next == ExportOrder.OrderStatus.CANCELLED) {
            Map<Long, InventoryLot> lockedLots = new LinkedHashMap<>();
            lines.stream().map(line -> line.getInventoryLot().getId()).distinct().sorted().forEach(lotId ->
                lockedLots.put(lotId, notFound(lots.findByIdForUpdate(lotId), "Lote de inventario no encontrado"))
            );
            for (ExportOrderItem line : lines) {
                InventoryLot lot = lockedLots.get(line.getInventoryLot().getId());
                lot.setQuantity(lot.getQuantity().add(line.getQuantityKg()));
                lot.setStatus(InventoryLot.LotStatus.AVAILABLE);
            }
            for (InventoryLot lot : lockedLots.values()) {
                lots.save(lot);
                for (ProductListing related : listings.findByInventoryLot_Id(lot.getId())) {
                    if (related.getStatus() == ProductListing.ListingStatus.SOLD_OUT && lot.getQuantity().compareTo(related.getMinimumOrderKg()) >= 0) {
                        related.setStatus(ProductListing.ListingStatus.PUBLISHED);
                        listings.save(related);
                    }
                }
            }
        }
        order.setStatus(next); orders.save(order); log(u, "ORDER_STATUS_CHANGED", "export_orders", orderId, Map.of("from", current.name(), "to", next.name())); return orderView(order);
    }

    private ListingView listingView(ProductListing l) { return new ListingView(l.getId(), l.getTitle(), l.getDescription(), l.getPriceUsdPerKg(), l.getMinimumOrderKg(), l.getStatus().name(), l.getCreatedAt(), new ProductOption(l.getProduct().getId(), l.getProduct().getName(), l.getProduct().getProductType(), l.getProduct().getDescription()), producerContact(l.getProducer()), lotOption(l.getInventoryLot())); }
    private ProducerContact producerContact(Producer p) { return new ProducerContact(p.getId(), p.getName(), p.getContact(), p.getEmail(), p.getPhone()); }
    private ClientContact clientContact(Client c) { return new ClientContact(c.getId(), c.getCompanyName(), c.getContactName(), c.getEmail(), c.getPhone(), c.getCountry(), c.getAddress()); }
    private LotOption lotOption(InventoryLot l) { return new LotOption(l.getId(), l.getLotCode(), l.getQuantity(), l.getStatus().name(), l.getHarvestDate(), l.getProduct().getId(), l.getProduct().getName()); }
    private CartView cartView(Cart cart) { List<CartItemView> views = cartItems.findByCart_Id(cart.getId()).stream().map(i -> new CartItemView(i.getId(), listingView(i.getListing()), i.getQuantityKg(), i.getListing().getPriceUsdPerKg().multiply(i.getQuantityKg()).setScale(2, RoundingMode.HALF_UP))).toList(); BigDecimal total = views.stream().map(CartItemView::lineTotalUsd).reduce(BigDecimal.ZERO, BigDecimal::add); String seller = views.isEmpty() ? null : views.get(0).listing().producer().name(); return new CartView(cart.getId(), cart.getStatus().name(), views, total, seller); }
    private OrderView orderView(ExportOrder o) { List<ExportOrderItem> lines = orderItems.findByExportOrder_Id(o.getId()); List<OrderLineView> lineViews = lines.stream().map(i -> new OrderLineView(i.getId(), i.getProduct().getName(), i.getProduct().getProductType(), i.getInventoryLot().getLotCode(), i.getQuantityKg(), i.getSalePriceUsdPerKg(), i.getQuantityKg().multiply(i.getSalePriceUsdPerKg()).setScale(2, RoundingMode.HALF_UP))).toList(); BigDecimal total = lineViews.stream().map(OrderLineView::totalUsd).reduce(BigDecimal.ZERO, BigDecimal::add); ProducerContact seller = lines.isEmpty() ? null : producerContact(lines.get(0).getInventoryLot().getProducer()); return new OrderView(o.getId(), o.getOrderCode(), o.getOrderDate(), o.getStatus().name(), o.getDestinationCountry(), o.getDestinationAddress(), o.getNotes(), total, clientContact(o.getClient()), seller, lineViews); }
}
