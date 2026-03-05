package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import gift.order.OrderRepository;
import gift.wish.WishRepository;
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
  private final CategoryService categoryService;
  private final OrderRepository orderRepository;
  private final WishRepository wishRepository;

  public ProductService(
      ProductRepository productRepository,
      CategoryService categoryService,
      OrderRepository orderRepository,
      WishRepository wishRepository) {
    this.productRepository = productRepository;
    this.categoryService = categoryService;
    this.orderRepository = orderRepository;
    this.wishRepository = wishRepository;
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
    return create(name, price, imageUrl, categoryId, false);
  }

  @Transactional
  public Product create(
      String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
    validateNameOrThrow(name, allowKakao);
    Category category =
        categoryService
            .findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
    return productRepository.save(new Product(name, price, imageUrl, category));
  }

  @Transactional
  public Optional<Product> update(
      Long id, String name, int price, String imageUrl, Long categoryId) {
    return update(id, name, price, imageUrl, categoryId, false);
  }

  @Transactional
  public Optional<Product> update(
      Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
    validateNameOrThrow(name, allowKakao);
    Category category =
        categoryService
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
    if (orderRepository.existsByOptionProductId(id)) {
      throw new IllegalStateException("주문 이력이 있는 상품은 삭제할 수 없습니다.");
    }
    wishRepository.deleteByProductId(id);
    productRepository.deleteById(id);
  }

  private void validateNameOrThrow(String name, boolean allowKakao) {
    List<String> errors = ProductNameValidator.validate(name, allowKakao);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join(", ", errors));
    }
  }

  public List<String> validateName(String name, boolean allowKakao) {
    return ProductNameValidator.validate(name, allowKakao);
  }
}
