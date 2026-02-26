package gift.wish;

import gift.auth.AuthenticationResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.NoSuchElementException;

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
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        var wishes = wishService.findByMemberId(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        var existing = wishService.findByMemberIdAndProductId(member.getId(), request.productId());
        if (existing.isPresent()) {
            return ResponseEntity.ok(WishResponse.from(existing.get()));
        }

        var saved = wishService.create(member.getId(), request.productId());
        return ResponseEntity.created(URI.create("/api/wishes/" + saved.getId()))
            .body(WishResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        wishService.remove(member.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Void> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(403).build();
    }
}
