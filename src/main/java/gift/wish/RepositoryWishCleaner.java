package gift.wish;

import org.springframework.stereotype.Component;

/**
 * 그 회원 + 상품의 위시를 모두 지운다. 같은 상품 위시가 여러 개여도 모두 지운다
 * (DB에 unique 제약이 없다, docs/work/08-wish-cleanup/tech-spec.md 2장).
 * 트랜잭션은 부르는 쪽(주문 커밋 뒤 리스너의 REQUIRES_NEW)이 연다.
 */
@Component
public class RepositoryWishCleaner implements WishCleaner {
    private final WishRepository wishRepository;

    public RepositoryWishCleaner(WishRepository wishRepository) {
        this.wishRepository = wishRepository;
    }

    @Override
    public void clean(Long memberId, Long productId) {
        wishRepository.deleteByMemberIdAndProductId(memberId, productId);
    }
}
