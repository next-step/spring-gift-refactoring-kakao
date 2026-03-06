package gift.option;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, Long> {
  List<Option> findByProductId(Long productId);

  long countByProductId(Long productId);

  boolean existsByProductIdAndName(Long productId, String name);
}
