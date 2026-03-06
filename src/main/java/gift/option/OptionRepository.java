package gift.option;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findByProductId(Long productId);

    boolean existsByProductIdAndName(Long productId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Option o where o.id = :id")
    Optional<Option> findByIdForUpdate(Long id);
}
