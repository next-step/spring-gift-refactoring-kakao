package gift.wish;

import gift.product.Product;
import gift.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductService productService;

    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    public Optional<Wish> findByMemberIdAndProductId(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId);
    }

    public Wish create(Long memberId, Long productId) {
        Product product = productService.findById(productId);
        return wishRepository.save(new Wish(memberId, product));
    }

    public void removeWish(Long id, Long memberId) {
        Wish wish = wishRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("위시를 찾을 수 없습니다. id: " + id));

        if (!wish.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 위시만 삭제할 수 있습니다.");
        }

        wishRepository.delete(wish);
    }
}
