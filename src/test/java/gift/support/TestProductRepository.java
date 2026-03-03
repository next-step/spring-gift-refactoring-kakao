package gift.support;

import gift.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestProductRepository extends JpaRepository<Product, Long> {

}
