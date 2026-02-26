package gift.wish;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
        final Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final Page<WishResponse> wishes =
                wishService.getWishes(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
            @RequestHeader("Authorization") String authorization, @Valid @RequestBody WishRequest request) {
        final Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final Optional<Wish> existing = wishService.findByMemberAndProduct(member.getId(), request.productId());
        if (existing.isPresent()) {
            return ResponseEntity.ok(WishResponse.from(existing.get()));
        }

        final Wish saved = wishService.createWish(member.getId(), request.productId());
        return ResponseEntity.created(URI.create("/api/wishes/" + saved.getId()))
                .body(WishResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
            @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        final Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final Wish wish = wishService.findById(id);
        if (!wish.getMemberId().equals(member.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        wishService.removeWish(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }
}
