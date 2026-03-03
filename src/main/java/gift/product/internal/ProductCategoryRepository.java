package gift.product.internal;

import gift.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<Category, Long> {

}
