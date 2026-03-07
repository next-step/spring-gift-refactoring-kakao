package gift.wish.internal;

import gift.wish.Wish;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishRepository extends JpaRepository<Wish, Long> {

    @EntityGraph(attributePaths = "product")
    Page<Wish> findByMemberId(Long memberId, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Optional<Wish> findByMemberIdAndProductId(Long memberId, Long productId);
}
