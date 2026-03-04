package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  public ProductService(
      ProductRepository productRepository, CategoryRepository categoryRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
  }

  public Page<Product> findAll(Pageable pageable) {
    return productRepository.findAll(pageable);
  }

  public List<Product> findAll() {
    return productRepository.findAll();
  }

  public Optional<Product> findById(Long id) {
    return productRepository.findById(id);
  }

  @Transactional
  public Product create(String name, int price, String imageUrl, Long categoryId) {
    validateNameOrThrow(name);
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
    return productRepository.save(new Product(name, price, imageUrl, category));
  }

  @Transactional
  public Optional<Product> update(
      Long id, String name, int price, String imageUrl, Long categoryId) {
    validateNameOrThrow(name);
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
    return productRepository
        .findById(id)
        .map(
            product -> {
              product.update(name, price, imageUrl, category);
              return product;
            });
  }

  @Transactional
  public void delete(Long id) {
    productRepository.deleteById(id);
  }

  private void validateNameOrThrow(String name) {
    List<String> errors = ProductNameValidator.validate(name);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join(", ", errors));
    }
  }

  public List<String> validateName(String name, boolean allowKakao) {
    return ProductNameValidator.validate(name, allowKakao);
  }
}
