package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Page<WishResponse> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable).map(WishResponse::from);
    }

    public Optional<AddWishResult> addWish(Long memberId, WishRequest request) {
        Product product = productRepository.findById(request.productId()).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        Wish existing = wishRepository.findByMemberIdAndProductId(memberId, product.getId()).orElse(null);
        if (existing != null) {
            return Optional.of(new AddWishResult(WishResponse.from(existing), false));
        }

        Wish saved = wishRepository.save(new Wish(memberId, product));
        return Optional.of(new AddWishResult(WishResponse.from(saved), true));
    }

    public record AddWishResult(WishResponse response, boolean created) {}

    public Optional<DeleteResult> removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId).orElse(null);
        if (wish == null) {
            return Optional.of(DeleteResult.NOT_FOUND);
        }

        if (!wish.getMemberId().equals(memberId)) {
            return Optional.of(DeleteResult.FORBIDDEN);
        }

        wishRepository.delete(wish);
        return Optional.of(DeleteResult.SUCCESS);
    }

    public enum DeleteResult {
        SUCCESS, NOT_FOUND, FORBIDDEN
    }
}
