package gift.wish;

import gift.auth.AuthenticationResolver;
import gift.error.ForbiddenException;
import gift.product.Product;
import gift.product.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;
    private final AuthenticationResolver authenticationResolver;

    @Autowired
    public WishController(
        WishRepository wishRepository,
        ProductRepository productRepository,
        AuthenticationResolver authenticationResolver
    ) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
        this.authenticationResolver = authenticationResolver;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        var member = authenticationResolver.extractMember(authorization);
        var wishes = wishRepository.findByMemberId(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        var member = authenticationResolver.extractMember(authorization);

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + request.productId()));

        /* 중복 위시 확인 */
        var existing = wishRepository.findByMemberIdAndProductId(member.getId(), product.getId()).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(WishResponse.from(existing));
        }

        var saved = wishRepository.save(request.toEntity(member.getId(), product));
        return ResponseEntity.created(URI.create("/api/wishes/" + saved.getId()))
            .body(WishResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id
    ) {
        var member = authenticationResolver.extractMember(authorization);

        var wish = wishRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("위시를 찾을 수 없습니다. id=" + id));

        if (!wish.getMemberId().equals(member.getId())) {
            throw new ForbiddenException("다른 회원의 위시를 삭제할 수 없습니다.");
        }

        wishRepository.delete(wish);
        return ResponseEntity.noContent().build();
    }
}
