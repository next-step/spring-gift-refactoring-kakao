package gift.wish;

import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public record AddResult(AddStatus status, Wish wish) {}

    public enum AddStatus {
        PRODUCT_NOT_FOUND,
        ALREADY_EXISTS,
        CREATED
    }

    public enum RemoveResult {
        NOT_FOUND,
        FORBIDDEN,
        DELETED
    }

    public Page<WishResponse> findWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable).map(WishResponse::from);
    }

    public AddResult addWish(Long memberId, Long productId) {
        // check product
        var product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return new AddResult(AddStatus.PRODUCT_NOT_FOUND, null);
        }

        // check duplicate
        var existing = wishRepository
                .findByMemberIdAndProductId(memberId, product.getId())
                .orElse(null);
        if (existing != null) {
            return new AddResult(AddStatus.ALREADY_EXISTS, existing);
        }

        var saved = wishRepository.save(new Wish(memberId, product));
        return new AddResult(AddStatus.CREATED, saved);
    }

    public RemoveResult removeWish(Long memberId, Long wishId) {
        var wish = wishRepository.findById(wishId).orElse(null);
        if (wish == null) {
            return RemoveResult.NOT_FOUND;
        }

        if (!wish.getMemberId().equals(memberId)) {
            return RemoveResult.FORBIDDEN;
        }

        wishRepository.delete(wish);
        return RemoveResult.DELETED;
    }
}
