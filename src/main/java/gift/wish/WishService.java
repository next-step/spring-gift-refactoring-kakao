package gift.wish;

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
    return productRepository
        .findById(productId)
        .map(
            product ->
                wishRepository
                    .findByMemberIdAndProductId(memberId, productId)
                    .map(existing -> new AddWishResult(existing, false))
                    .orElseGet(
                        () -> {
                          Wish saved = wishRepository.save(new Wish(memberId, product));
                          return new AddWishResult(saved, true);
                        }));
  }

  @Transactional
  public RemoveWishResult removeWish(Long memberId, Long wishId) {
    return wishRepository
        .findById(wishId)
        .map(
            wish -> {
              if (!wish.getMemberId().equals(memberId)) {
                return RemoveWishResult.FORBIDDEN;
              }
              wishRepository.delete(wish);
              return RemoveWishResult.DELETED;
            })
        .orElse(RemoveWishResult.NOT_FOUND);
  }

  public record AddWishResult(Wish wish, boolean created) {}

  public enum RemoveWishResult {
    DELETED,
    NOT_FOUND,
    FORBIDDEN
  }
}
