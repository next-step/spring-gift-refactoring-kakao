package gift.wish;

import gift.ForbiddenException;
import gift.product.Product;
import gift.product.ProductRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Page<Wish> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Wish addWish(Long memberId, Long productId) {
        final Optional<Wish> existing = wishRepository.findByMemberIdAndProductId(memberId, productId);
        if (existing.isPresent()) {
            return existing.get();
        }
        final Product product =
                productRepository.findById(productId).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
        return wishRepository.save(new Wish(memberId, product));
    }

    public boolean existsWishByMemberAndProduct(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId).isPresent();
    }

    @Transactional
    public void removeWishByMemberAndProduct(Long memberId, Long productId) {
        final Wish wish = wishRepository
                .findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다."));
        wishRepository.delete(wish);
    }

    @Transactional
    public void removeWish(Long wishId, Long memberId) {
        final Wish wish =
                wishRepository.findById(wishId).orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다."));
        if (!wish.isOwnedBy(memberId)) {
            throw new ForbiddenException("본인의 위시만 삭제할 수 있습니다.");
        }
        wishRepository.deleteById(wishId);
    }
}
