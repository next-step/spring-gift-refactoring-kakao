package gift.option.internal;

import gift.product.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OptionProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select p from Product p
            left join fetch p.options
                where p.id = :id
            """)
    Optional<Product> findByIdLeftJoinFetchOptions(Long id);
}
