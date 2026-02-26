package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public List<Product> findAllEntities() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + id));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public Product findEntityById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + id));
    }

    public List<String> validateProductName(String name, boolean allowKakao) {
        return ProductNameValidator.validate(name, allowKakao);
    }

    @Transactional
    public ProductResponse create(String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다. id=" + categoryId));

        Product saved = productRepository.save(new Product(name, price, imageUrl, category));
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다. id=" + categoryId));

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + id));

        product.update(name, price, imageUrl, category);
        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
