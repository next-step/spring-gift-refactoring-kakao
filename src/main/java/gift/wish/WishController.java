package gift.wish;

import gift.auth.AuthenticatedMember;
import gift.member.Member;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        @AuthenticatedMember Member member,
        Pageable pageable
    ) {
        Page<WishResponse> wishes = wishService.getWishes(member.getId(), pageable);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @AuthenticatedMember Member member,
        @Valid @RequestBody WishRequest request
    ) {
        return wishService.addWish(member.getId(), request)
            .map(result -> {
                if (result.created()) {
                    return ResponseEntity
                        .created(URI.create("/api/wishes/" + result.response().id()))
                        .body(result.response());
                }
                return ResponseEntity.ok(result.response());
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @AuthenticatedMember Member member,
        @PathVariable Long id
    ) {
        return switch (wishService.removeWish(member.getId(), id)) {
            case SUCCESS -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        };
    }
}
