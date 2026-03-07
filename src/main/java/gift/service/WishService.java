package gift.service;

import gift.exception.ForbiddenException;
import gift.model.Product;
import gift.model.Wish;
import gift.repository.WishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductService productService;

    public WishService(WishRepository wishRepository, ProductService productService) {
        this.wishRepository = wishRepository;
        this.productService = productService;
    }

    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    public boolean existsByMemberIdAndProductId(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId).isPresent();
    }

    public Wish addWish(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId)
            .orElseGet(() -> {
                Product product = productService.findById(productId);
                return wishRepository.save(new Wish(memberId, product));
            });
    }

    public void removeByMemberIdAndProductId(Long memberId, Long productId) {
        wishRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    public void removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("Wish not found. id=" + wishId));

        if (!wish.getMemberId().equals(memberId)) {
            throw new ForbiddenException("Cannot delete another member's wish.");
        }

        wishRepository.delete(wish);
    }
}
