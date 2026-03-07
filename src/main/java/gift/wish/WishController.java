package gift.wish;

import gift.auth.AuthenticatedMember;
import gift.auth.MemberPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;

    public WishController(WishService wishService) {
        this.wishService = wishService;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @AuthenticatedMember MemberPrincipal principal,
        Pageable pageable
    ) {
        Page<WishResponse> wishes = wishService.getWishes(principal.id(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @AuthenticatedMember MemberPrincipal principal,
        @Valid @RequestBody WishRequest request
    ) {
        Wish wish = wishService.addWish(principal.id(), request.productId());
        return ResponseEntity.ok(WishResponse.from(wish));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @AuthenticatedMember MemberPrincipal principal,
        @PathVariable Long id
    ) {
        wishService.removeWish(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
