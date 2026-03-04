package gift.wish;

import gift.product.Product;
import gift.product.ProductRepository;
import java.util.Optional;
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
  public Optional<AddWishResult> addWish(Long memberId, Long productId) {
    Product product = productRepository.findById(productId).orElse(null);
    if (product == null) {
      return Optional.empty();
    }

    Wish existing = wishRepository.findByMemberIdAndProductId(memberId, productId).orElse(null);
    if (existing != null) {
      return Optional.of(new AddWishResult(existing, false));
    }

    Wish saved = wishRepository.save(new Wish(memberId, product));
    return Optional.of(new AddWishResult(saved, true));
  }

  @Transactional
  public RemoveWishResult removeWish(Long memberId, Long wishId) {
    Wish wish = wishRepository.findById(wishId).orElse(null);
    if (wish == null) {
      return RemoveWishResult.NOT_FOUND;
    }

    if (!wish.getMemberId().equals(memberId)) {
      return RemoveWishResult.FORBIDDEN;
    }

    wishRepository.delete(wish);
    return RemoveWishResult.DELETED;
  }

  public record AddWishResult(Wish wish, boolean created) {}

  public enum RemoveWishResult {
    DELETED,
    NOT_FOUND,
    FORBIDDEN
  }
}
