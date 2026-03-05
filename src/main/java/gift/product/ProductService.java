package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  public ProductService(
      ProductRepository productRepository, CategoryRepository categoryRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
  }

  public List<Product> findAllProducts() {
    return productRepository.findAll();
  }

  public Optional<Product> findProductById(Long id) {
    return productRepository.findById(id);
  }

  public List<String> validateName(String name, boolean allowKakao) {
    return ProductNameValidator.validate(name, allowKakao);
  }

  public Product saveProduct(String name, int price, String imageUrl, Long categoryId) {
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
    return productRepository.save(new Product(name, price, imageUrl, category));
  }

  public Product updateProduct(Long id, String name, int price, String imageUrl, Long categoryId) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
    product.update(name, price, imageUrl, category);
    return productRepository.save(product);
  }

  public Page<ProductResponse> getAllProducts(Pageable pageable) {
    return productRepository.findAll(pageable).map(ProductResponse::from);
  }

  public Optional<ProductResponse> getProduct(Long id) {
    return productRepository.findById(id).map(ProductResponse::from);
  }

  public Optional<ProductResponse> createProduct(ProductRequest request) {
    validateName(request.name());

    return categoryRepository
        .findById(request.categoryId())
        .map(
            category -> {
              Product saved = productRepository.save(request.toEntity(category));
              return ProductResponse.from(saved);
            });
  }

  public Optional<ProductResponse> updateProduct(Long id, ProductRequest request) {
    validateName(request.name());

    Optional<Category> categoryOpt = categoryRepository.findById(request.categoryId());
    if (categoryOpt.isEmpty()) {
      return Optional.empty();
    }

    return productRepository
        .findById(id)
        .map(
            product -> {
              product.update(
                  request.name(), request.price(), request.imageUrl(), categoryOpt.get());
              Product saved = productRepository.save(product);
              return ProductResponse.from(saved);
            });
  }

  public void deleteProduct(Long id) {
    productRepository.deleteById(id);
  }

  private void validateName(String name) {
    List<String> errors = ProductNameValidator.validate(name);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join(", ", errors));
    }
  }
}
