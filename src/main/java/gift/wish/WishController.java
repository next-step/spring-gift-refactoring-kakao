package gift.wish;

import gift.auth.AuthenticationResolver;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;
    private final AuthenticationResolver authenticationResolver;

    public WishController(WishService wishService, AuthenticationResolver authenticationResolver) {
        this.wishService = wishService;
        this.authenticationResolver = authenticationResolver;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
            @RequestHeader("Authorization") String authorization, Pageable pageable) {
        // check auth
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        var wishes = wishService.findWishes(member.getId(), pageable);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
            @RequestHeader("Authorization") String authorization, @Valid @RequestBody WishRequest request) {
        // check auth
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        var result = wishService.addWish(member.getId(), request.productId());
        return switch (result.status()) {
            case PRODUCT_NOT_FOUND -> ResponseEntity.notFound().build();
            case ALREADY_EXISTS -> ResponseEntity.ok(WishResponse.from(result.wish()));
            case CREATED ->
                ResponseEntity.created(URI.create("/api/wishes/" + result.wish().getId()))
                        .body(WishResponse.from(result.wish()));
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
            @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        // check auth
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        return switch (wishService.removeWish(member.getId(), id)) {
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case FORBIDDEN -> ResponseEntity.status(403).build();
            case DELETED -> ResponseEntity.noContent().build();
        };
    }
}
