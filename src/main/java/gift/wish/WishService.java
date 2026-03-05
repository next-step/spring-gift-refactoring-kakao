package gift.wish;

import gift.product.Product;
import gift.product.ProductService;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductService productService;

    public WishService(WishRepository wishRepository, ProductService productService) {
        this.wishRepository = wishRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public AddWishResult addWish(Long memberId, Long productId) {
        Product product = productService.findById(productId);

        var existing = wishRepository
                .findByMemberIdAndProductId(memberId, product.getId())
                .orElse(null);
        if (existing != null) {
            return new AddWishResult(existing, false);
        }

        try {
            Wish saved = wishRepository.save(new Wish(memberId, product));
            return new AddWishResult(saved, true);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 위시리스트에 추가된 상품입니다.");
        }
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
