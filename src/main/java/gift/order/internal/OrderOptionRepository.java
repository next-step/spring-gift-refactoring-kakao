package gift.order.internal;

import gift.option.Option;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderOptionRepository extends JpaRepository<Option, Long> {

    @Query("""
            select o from Option o
            inner join fetch o.product
                where o.id = :id
            """)
    Optional<Option> findByIdInnerJoinFetchProduct(Long id);
}
