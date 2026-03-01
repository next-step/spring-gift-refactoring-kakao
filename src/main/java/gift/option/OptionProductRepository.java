package gift.option;

import gift.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionProductRepository extends JpaRepository<Product, Long> {

}
