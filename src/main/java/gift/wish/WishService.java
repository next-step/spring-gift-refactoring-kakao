package gift.wish;

import gift.error.EntityNotFoundException;
import gift.error.ForbiddenException;
import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    public WishResult addWish(Long memberId, WishRequest request) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));

        Optional<Wish> existing = wishRepository.findByMemberIdAndProductId(memberId, product.getId());
        if (existing.isPresent()) {
            return new WishResult(existing.get(), false);
        }

        Wish saved = wishRepository.save(new Wish(memberId, product));
        return new WishResult(saved, true);
    }

    @Transactional
    public void removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new EntityNotFoundException("위시가 존재하지 않습니다."));

        if (!wish.getMemberId().equals(memberId)) {
            throw new ForbiddenException("본인의 위시만 삭제할 수 있습니다.");
        }

        wishRepository.delete(wish);
    }

    public record WishResult(Wish wish, boolean created) {
    }
}
