package gift.support;

import gift.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestOrderRepository extends JpaRepository<Order, Long> {

}
