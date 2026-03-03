package gift.wish.internal;

import gift.wish.Wish;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WishRepository extends JpaRepository<Wish, Long> {

    @Query(
            value = """
                        select w from Wish w
                        inner join fetch w.product
                            where w.memberId = :memberId
                    """,
            countQuery = """
                        select count(w) from Wish w
                            where w.memberId = :memberId
                    """
    )
    Page<Wish> findByMemberIdInnerJoinFetchProduct(Long memberId, Pageable pageable);

    @Query("""
            select w from Wish w
            inner join fetch w.product
                where w.memberId = :memberId
                and w.product.id = :productId
            """)
    Optional<Wish> findByMemberIdAndProductIdInnerJoinFetchProduct(Long memberId, Long productId);
}
