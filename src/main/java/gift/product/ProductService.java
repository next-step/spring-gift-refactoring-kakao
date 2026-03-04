package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
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

    public Product getById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    public Product create(ProductRequest request) {
        return create(request, false);
    }

    public Product create(ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Category category = findCategoryById(request.categoryId());
        return productRepository.save(request.toEntity(category));
    }

    public Product update(Long id, ProductRequest request) {
        return update(id, request, false);
    }

    public Product update(Long id, ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Category category = findCategoryById(request.categoryId());
        Product product = getById(id);
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return productRepository.save(product);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public List<Category> findAllCategories() {
        return categoryService.findAll();
    }

    private Category findCategoryById(Long categoryId) {
        return categoryService.findById(categoryId);
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
