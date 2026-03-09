package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public record AddWishResult(Wish wish, boolean created) {}

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public AddWishResult addWish(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. productId=" + productId));

        Optional<Wish> existing = wishRepository.findByMemberIdAndProductId(memberId, productId);
        if (existing.isPresent()) {
            return new AddWishResult(existing.get(), false);
        }

        return new AddWishResult(wishRepository.save(new Wish(memberId, product)), true);
    }

    @Transactional
    public void removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다. id=" + wishId));

        wish.validateOwnership(memberId);

        wishRepository.delete(wish);
    }
}
