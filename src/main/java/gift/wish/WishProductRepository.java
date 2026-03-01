package gift.wish;

import gift.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishProductRepository extends JpaRepository<Product, Long> {

}
