package gift.product.admin;

import gift.product.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select p from Product p
            inner join fetch p.category
            """)
    List<Product> findAllInnerJoinFetchCategory();

    @Query("""
            select p from Product p
            inner join fetch p.category
                where p.id = :id
            """)
    Optional<Product> findByIdInnerJoinFetchCategory(Long id);
}
