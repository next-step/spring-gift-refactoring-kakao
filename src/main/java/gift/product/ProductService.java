package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProduct(Long id) {
        return productRepository.findById(id)
            .map(ProductResponse::from);
    }

    public Optional<ProductResponse> createProduct(ProductRequest request) {
        validateName(request.name());

        return categoryRepository.findById(request.categoryId())
            .map(category -> {
                Product saved = productRepository.save(request.toEntity(category));
                return ProductResponse.from(saved);
            });
    }

    public Optional<ProductResponse> updateProduct(Long id, ProductRequest request) {
        validateName(request.name());

        return categoryRepository.findById(request.categoryId())
            .flatMap(category -> productRepository.findById(id)
                .map(product -> {
                    product.update(request.name(), request.price(), request.imageUrl(), category);
                    Product saved = productRepository.save(product);
                    return ProductResponse.from(saved);
                }));
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

    // Admin operations

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. id=" + id));
    }

    public Product create(String name, int price, String imageUrl, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다. id=" + categoryId));
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    public Product update(Long id, String name, int price, String imageUrl, Long categoryId) {
        Product product = findById(id);
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다. id=" + categoryId));
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    public List<String> validateNameForAdmin(String name) {
        return ProductNameValidator.validate(name, true);
    }
}
