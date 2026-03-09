package gift.wish;

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

import gift.auth.AuthenticatedMember;
import gift.member.Member;
import jakarta.validation.Valid;

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
        return ResponseEntity.ok(wishService.findByMemberId(member.getId(), pageable).map(WishResponse::from));
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @AuthenticatedMember Member member,
        @Valid @RequestBody WishRequest request
    ) {
        WishResult result = wishService.add(member.getId(), request);
        WishResponse response = WishResponse.from(result.getWish());

        if (!result.isCreated()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.created(URI.create("/api/wishes/" + response.id()))
            .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @AuthenticatedMember Member member,
        @PathVariable Long id
    ) {
        wishService.remove(member.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
