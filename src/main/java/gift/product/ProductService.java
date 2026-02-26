package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    public Optional<ProductResponse> getProduct(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return Optional.empty();
        }
        return Optional.of(ProductResponse.from(product));
    }

    public Optional<ProductResponse> createProduct(ProductRequest request) {
        validateName(request.name());

        Category category = categoryRepository.findById(request.categoryId()).orElse(null);
        if (category == null) {
            return Optional.empty();
        }

        Product saved = productRepository.save(request.toEntity(category));
        return Optional.of(ProductResponse.from(saved));
    }

    public Optional<ProductResponse> updateProduct(Long id, ProductRequest request) {
        validateName(request.name());

        Category category = categoryRepository.findById(request.categoryId()).orElse(null);
        if (category == null) {
            return Optional.empty();
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        product.update(request.name(), request.price(), request.imageUrl(), category);
        Product saved = productRepository.save(product);
        return Optional.of(ProductResponse.from(saved));
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
