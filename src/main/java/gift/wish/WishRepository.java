package gift.wish;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {
    Page<Wish> findByMemberId(Long memberId, Pageable pageable);

    // product가 @ManyToOne 관계라 Query Creation이 product.id 경로를 자동 해석하지 못함 (H2 호환 문제)
    @Query("SELECT w FROM Wish w WHERE w.memberId = :memberId AND w.product.id = :productId")
    Optional<Wish> findByMemberIdAndProductId(Long memberId, Long productId);

    @Modifying
    @Query("DELETE FROM Wish w WHERE w.memberId = :memberId AND w.product.id = :productId")
    void deleteByMemberIdAndProductId(Long memberId, Long productId);
}
