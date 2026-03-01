package gift.order;

import gift.option.Option;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOptionRepository extends JpaRepository<Option, Long> {

}
