package gift.wish.internal;

import gift.auth.AuthenticationPort;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;
    private final AuthenticationPort authenticationPort;

    @GetMapping
    public ResponseEntity<PagedModel<WishResponse>> getWishes(
            @RequestHeader("Authorization") String authorization,
            Pageable pageable
    ) {
        // check auth
        Long memberId = authenticationPort.getMemberIdFrom(authorization);

        PagedModel<WishResponse> response = wishService.getWishes(memberId, pageable);

        return ResponseEntity
                .ok(response);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody WishRequest request
    ) {
        // check auth
        Long memberId = authenticationPort.getMemberIdFrom(authorization);

        AddWishResponseDto addWishResponseDto = wishService.addWish(memberId, request);

        WishResponse response = addWishResponseDto.response();
        boolean created = addWishResponseDto.created();

        if (!created) {
            return ResponseEntity
                    .ok(response);
        }

        Long wishId = response.id();

        return ResponseEntity.created(
                        URI.create("/api/wishes/" + wishId)
                )
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        // check auth
        Long memberId = authenticationPort.getMemberIdFrom(authorization);

        wishService.removeWish(memberId, id);

        return ResponseEntity
                .noContent()
                .build();
    }
}
