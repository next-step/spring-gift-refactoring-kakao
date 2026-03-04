package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
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

    @Transactional(readOnly = true)
    public Wish findByMemberIdAndProductId(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId).orElse(null);
    }

    @Transactional
    public Wish addWish(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new WishException(WishErrorCode.PRODUCT_NOT_FOUND));
        return wishRepository.save(new Wish(memberId, product));
    }

    @Transactional(readOnly = true)
    public Wish findById(Long id) {
        return wishRepository.findById(id)
            .orElseThrow(() -> new WishException(WishErrorCode.WISH_NOT_FOUND));
    }

    @Transactional
    public void delete(Wish wish) {
        wishRepository.delete(wish);
    }
}
