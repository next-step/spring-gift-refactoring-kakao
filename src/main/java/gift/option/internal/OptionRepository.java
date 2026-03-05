package gift.option.internal;

import gift.option.Option;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
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

    boolean existsByProductIdAndNameAndIdNot(Long productId, String name, Long optionId);

    List<Option> findAllByProductId(Long productId);

    long countByProductId(Long productId);

    @EntityGraph(attributePaths = "product")
    Optional<Option> findWithProductById(Long id);
}
