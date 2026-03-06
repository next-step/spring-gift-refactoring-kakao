package gift.category;

import gift.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  public CategoryService(
      CategoryRepository categoryRepository, ProductRepository productRepository) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  public List<Category> findAll() {
    return categoryRepository.findAll();
  }

  public Optional<Category> findById(Long id) {
    return categoryRepository.findById(id);
  }

  @Transactional
  public Category create(String name, String color, String imageUrl, String description) {
    return categoryRepository.save(new Category(name, color, imageUrl, description));
  }

  @Transactional
  public Optional<Category> update(
      Long id, String name, String color, String imageUrl, String description) {
    return categoryRepository
        .findById(id)
        .map(
            category -> {
              category.update(name, color, imageUrl, description);
              return category;
            });
  }

  @Transactional
  public void delete(Long id) {
    if (productRepository.existsByCategoryId(id)) {
      throw new IllegalStateException("해당 카테고리에 상품이 존재하여 삭제할 수 없습니다.");
    }
    categoryRepository.deleteById(id);
  }
}
