package gift.wish;

import gift.exception.ForbiddenException;
import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<Wish> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Wish addWish(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId)
            .orElseGet(() -> {
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
                return wishRepository.save(new Wish(memberId, product));
            });
    }

    @Transactional
    public void removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다. id=" + wishId));
        if (!wish.getMemberId().equals(memberId)) {
            throw new ForbiddenException("해당 위시에 대한 권한이 없습니다.");
        }
        wishRepository.delete(wish);
    }
}
