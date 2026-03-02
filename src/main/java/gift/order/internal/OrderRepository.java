package gift.order.internal;

import gift.order.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByMemberId(Long memberId, Pageable pageable);

    @Query("""
            select o from Order o
            inner join fetch o.option
            inner join fetch o.option.product
                where o.id = :id
            """)
    Optional<Order> findByIdInnerJoinFetchOptionAndProduct(Long id);
}
