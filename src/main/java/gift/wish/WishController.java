package gift.wish;

import gift.auth.Login;
import gift.member.Member;
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
        @Login Member member,
        Pageable pageable
    ) {
        Page<WishResponse> wishes = wishService.getWishes(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @Login Member member,
        @Valid @RequestBody WishRequest request
    ) {
        Wish wish = wishService.addWish(member.getId(), request.productId());
        return ResponseEntity.ok(WishResponse.from(wish));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @Login Member member,
        @PathVariable Long id
    ) {
        wishService.removeWish(member.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
