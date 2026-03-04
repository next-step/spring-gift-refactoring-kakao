package gift.wish;

import gift.auth.Authenticated;
import gift.member.Member;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;

    public WishController(WishService wishService) {
        this.wishService = wishService;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @Authenticated Member member,
        Pageable pageable
    ) {
        Page<WishResponse> wishes = wishService.getWishes(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @Authenticated Member member,
        @Valid @RequestBody WishRequest request
    ) {
        WishService.WishResult result = wishService.addWish(member.getId(), request);
        if (!result.created()) {
            return ResponseEntity.ok(WishResponse.from(result.wish()));
        }
        return ResponseEntity.created(URI.create("/api/wishes/" + result.wish().getId()))
            .body(WishResponse.from(result.wish()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @Authenticated Member member,
        @PathVariable Long id
    ) {
        wishService.removeWish(member.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
