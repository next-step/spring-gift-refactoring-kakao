package gift.support;

import gift.wish.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestWishRepository extends JpaRepository<Wish, Long> {

}
