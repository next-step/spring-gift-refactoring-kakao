package gift.wish;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;
    private final AuthenticationResolver authenticationResolver;

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
            @RequestHeader("Authorization") String authorization,
            Pageable pageable
    ) {
        Member member = authenticate(authorization);
        Page<WishResponse> wishes = wishService.findByMemberId(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody WishRequest request
    ) {
        Member member = authenticate(authorization);

        try {
            Optional<Wish> existing = wishService.findByMemberIdAndProductId(member.getId(), request.productId());
            if (existing.isPresent()) {
                return ResponseEntity.ok(WishResponse.from(existing.get()));
            }

            Wish saved = wishService.create(member.getId(), request.productId());
            return ResponseEntity.created(URI.create("/api/wishes/" + saved.getId()))
                    .body(WishResponse.from(saved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        Member member = authenticate(authorization);

        try {
            wishService.removeWish(id, member.getId());
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        }
    }

    private Member authenticate(String authorization) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new IllegalArgumentException("인증에 실패했습니다.");
        }
        return member;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleUnauthorized(IllegalArgumentException e) {
        return ResponseEntity.status(401).build();
    }
}
