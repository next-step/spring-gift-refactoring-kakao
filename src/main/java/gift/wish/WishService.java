package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public AddWishResult addWish(Long memberId, Long productId) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));

        var existing = wishRepository
                .findByMemberIdAndProductId(memberId, product.getId())
                .orElse(null);
        if (existing != null) {
            return new AddWishResult(existing, false);
        }

        Wish saved = wishRepository.save(new Wish(memberId, product));
        return new AddWishResult(saved, true);
    }

    @Transactional
    public void removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository
                .findById(wishId)
                .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다. id=" + wishId));

        if (!wish.getMemberId().equals(memberId)) {
            throw new IllegalStateException("다른 회원의 위시를 삭제할 수 없습니다.");
        }

        wishRepository.delete(wish);
    }
}
