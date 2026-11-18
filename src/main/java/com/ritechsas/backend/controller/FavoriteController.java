package com.ritechsas.backend.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;
import com.ritechsas.backend.entity.Favorite;
import com.ritechsas.backend.repository.*;

@RestController
@RequestMapping("/api/marketplace/favorites")
public class FavoriteController {
    private final FavoriteRepository favorites; private final UserRepository users; private final ProductListingRepository listings;
    public FavoriteController(FavoriteRepository f,UserRepository u,ProductListingRepository l){favorites=f;users=u;listings=l;}
    private Long userId(Authentication a){return users.findByUsername(a.getName()).orElseThrow().getId();}
    @GetMapping public List<Favorite> mine(Authentication a){return favorites.findByUserIdOrderByCreatedAtDesc(userId(a));}
    @PostMapping("/{listingId}") @ResponseStatus(HttpStatus.CREATED)
    public Favorite add(Authentication a,@PathVariable Long listingId){
        Long uid=userId(a);
        var listing = listings.findById(listingId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Publicación no encontrada"));
        if (listing.getStatus() != com.ritechsas.backend.entity.ProductListing.ListingStatus.PUBLISHED
                || !"ACTIVE".equalsIgnoreCase(listing.getProducer().getSellerStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes guardar publicaciones activas y disponibles");
        }
        if(favorites.existsByUserIdAndListingId(uid,listingId)) return favorites.findByUserIdOrderByCreatedAtDesc(uid).stream().filter(f->f.getListingId().equals(listingId)).findFirst().orElseThrow();
        Favorite f=new Favorite();f.setUserId(uid);f.setListingId(listingId);return favorites.save(f);
    }
    @DeleteMapping("/{listingId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(Authentication a,@PathVariable Long listingId){favorites.deleteByUserIdAndListingId(userId(a),listingId);}
}
