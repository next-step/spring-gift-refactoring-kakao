package gift.option.internal;

import gift.option.Option;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OptionRepository extends JpaRepository<Option, Long> {

    @Query("""
            select o from Option o
                where o.id = :optionId
                and o.product.id = :productId
            """)
    Optional<Option> findByIdAndProductId(Long optionId, Long productId);

    boolean existsByProductIdAndName(Long productId, String name);

    List<Option> findAllByProductId(Long productId);

    long countByProductId(Long productId);

    @Query("""
            select o from Option o
            inner join fetch o.product
                where o.id = :id
            """)
    Optional<Option> findByIdInnerJoinFetchProduct(Long id);
}
