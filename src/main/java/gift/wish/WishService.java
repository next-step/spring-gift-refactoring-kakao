package gift.wish;

import gift.error.ForbiddenException;
import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    @Autowired
    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<WishResponse> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable).map(WishResponse::from);
    }

    @Transactional
    public WishResult addWish(Long memberId, WishRequest request) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + request.productId()));

        var existing = wishRepository.findByMemberIdAndProductId(memberId, product.getId()).orElse(null);
        if (existing != null) {
            return new WishResult(WishResponse.from(existing), false);
        }

        var saved = wishRepository.save(request.toEntity(memberId, product));
        return new WishResult(WishResponse.from(saved), true);
    }

    @Transactional
    public void removeWish(Long memberId, Long wishId) {
        var wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시를 찾을 수 없습니다. id=" + wishId));

        if (!wish.getMemberId().equals(memberId)) {
            throw new ForbiddenException("다른 회원의 위시를 삭제할 수 없습니다.");
        }

        wishRepository.delete(wish);
    }

    public record WishResult(WishResponse response, boolean isNew) {}
}
