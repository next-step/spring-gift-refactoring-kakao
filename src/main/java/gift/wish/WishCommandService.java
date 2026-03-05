package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishCommandService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishCommandService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Wish addWish(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new WishException(WishErrorCode.PRODUCT_NOT_FOUND));
        return wishRepository.save(new Wish(memberId, product));
    }

    public void delete(Wish wish) {
        wishRepository.delete(wish);
    }
}
