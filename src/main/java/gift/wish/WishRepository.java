package gift.wish;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishRepository extends JpaRepository<Wish, Long> {
    Page<Wish> findByMemberId(Long memberId, Pageable pageable);

    Optional<Wish> findByMemberIdAndProductId(Long memberId, Long productId);

    // 그 회원 + 상품의 위시를 모두 지우고 지운 개수를 돌려준다. 트랜잭션 안에서 불러야 한다(docs/work/08-wish-cleanup/tech-spec.md 2장).
    long deleteByMemberIdAndProductId(Long memberId, Long productId);
}
