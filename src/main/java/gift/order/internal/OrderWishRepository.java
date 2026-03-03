package gift.order.internal;

import gift.wish.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderWishRepository extends JpaRepository<Wish, Long> {

}
