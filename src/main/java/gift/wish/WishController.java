package gift.wish;

import gift.auth.AuthMember;
import gift.member.Member;
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
    public ResponseEntity<Page<WishResponse>> getWishes(@AuthMember Member member, Pageable pageable) {
        final Page<WishResponse> wishes =
                wishService.getWishes(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(@AuthMember Member member, @Valid @RequestBody WishRequest request) {
        final Wish wish = wishService.addWish(member.getId(), request.productId());
        return ResponseEntity.created(URI.create("/api/wishes/" + wish.getId())).body(WishResponse.from(wish));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(@AuthMember Member member, @PathVariable Long id) {
        wishService.removeWish(id, member.getId());
        return ResponseEntity.noContent().build();
    }
}
