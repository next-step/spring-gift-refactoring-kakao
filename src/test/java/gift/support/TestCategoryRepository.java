package gift.support;

import gift.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCategoryRepository extends JpaRepository<Category, Long> {

}
