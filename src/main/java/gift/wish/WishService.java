package gift.wish;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.product.Product;
import gift.product.ProductRepository;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<WishResponse> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable).map(WishResponse::from);
    }

    @Transactional
    public WishResult add(Long memberId, WishRequest request) {
        // check product
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));

        // check duplicate
        Optional<Wish> existing = wishRepository.findByMemberIdAndProductId(memberId, product.getId());
        if (existing.isPresent()) {
            return new WishResult(WishResponse.from(existing.get()), false);
        }

        Wish saved = wishRepository.save(new Wish(memberId, product));
        return new WishResult(WishResponse.from(saved), true);
    }

    @Transactional
    public void remove(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다."));

        if (!wish.equalsMemberId(memberId)) {
            throw new IllegalAccessException("다른 회원의 위시를 삭제할 수 없습니다.");
        }

        wishRepository.delete(wish);
    }

    public static class IllegalAccessException extends RuntimeException {
        public IllegalAccessException(String message) {
            super(message);
        }
    }
}
