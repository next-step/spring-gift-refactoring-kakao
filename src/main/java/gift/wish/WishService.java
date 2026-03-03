package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
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

    public Page<Wish> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    public Optional<Wish> findByMemberAndProduct(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId);
    }

    @Transactional
    public Wish createWish(Long memberId, Long productId) {
        final Product product =
                productRepository.findById(productId).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
        return wishRepository.save(new Wish(memberId, product));
    }

    public Wish findById(Long id) {
        return wishRepository.findById(id).orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다."));
    }

    @Transactional
    public void removeWish(Long id) {
        wishRepository.deleteById(id);
    }
}
