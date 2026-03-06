package gift.wish;

import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<WishResponse> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable).map(WishResponse::from);
    }

    public Optional<AddWishResult> addWish(Long memberId, WishRequest request) {
        return productRepository.findById(request.productId())
            .map(product -> {
                Optional<Wish> existing = wishRepository.findByMemberIdAndProductId(memberId, product.getId());
                if (existing.isPresent()) {
                    return new AddWishResult(WishResponse.from(existing.get()), false);
                }
                Wish saved = wishRepository.save(new Wish(memberId, product));
                return new AddWishResult(WishResponse.from(saved), true);
            });
    }

    public record AddWishResult(WishResponse response, boolean created) {}

    public DeleteResult removeWish(Long memberId, Long wishId) {
        Optional<Wish> wishOpt = wishRepository.findById(wishId);
        if (wishOpt.isEmpty()) {
            return DeleteResult.NOT_FOUND;
        }

        Wish wish = wishOpt.get();
        if (!wish.getMemberId().equals(memberId)) {
            return DeleteResult.FORBIDDEN;
        }

        wishRepository.delete(wish);
        return DeleteResult.SUCCESS;
    }

    public enum DeleteResult {
        SUCCESS, NOT_FOUND, FORBIDDEN
    }
}
