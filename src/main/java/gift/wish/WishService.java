package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
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

    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public AddResult add(Long memberId, Long productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        return wishRepository.findByMemberIdAndProductId(memberId, productId)
            .map(existing -> new AddResult(existing, false))
            .orElseGet(() -> new AddResult(wishRepository.save(new Wish(memberId, product)), true));
    }

    @Transactional
    public void remove(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId).orElseThrow();
        if (!wish.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 위시리스트만 삭제할 수 있습니다.");
        }
        wishRepository.delete(wish);
    }

    public record AddResult(Wish wish, boolean created) {
    }
}
