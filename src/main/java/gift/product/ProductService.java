package gift.product;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.category.Category;
import gift.category.CategoryRepository;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Long categoryId, Pageable pageable) {
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId, pageable).map(ProductResponse::from);
        }
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        return create(request, NamePolicy.STANDARD);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, NamePolicy namePolicy) {
        validateName(request.name(), namePolicy);
        Category category = findCategory(request.categoryId());
        Product saved = productRepository.save(request.toEntity(category));
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        return update(id, request, NamePolicy.STANDARD);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, NamePolicy namePolicy) {
        validateName(request.name(), namePolicy);
        Category category = findCategory(request.categoryId());
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
        product.update(request.name(), request.price(), request.imageUrl(), category);
        productRepository.save(product);
        return ProductResponse.from(product);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다."));
    }

    private void validateName(String name, NamePolicy namePolicy) {
        List<String> errors = ProductNameValidator.validate(name, namePolicy);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
