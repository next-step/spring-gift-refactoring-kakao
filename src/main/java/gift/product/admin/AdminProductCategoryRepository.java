package gift.product.admin;

import gift.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProductCategoryRepository extends JpaRepository<Category, Long> {

}
