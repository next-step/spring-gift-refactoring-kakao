package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<Product> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional
    public Product createProduct(String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateName(name, allowKakao);
        final Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다."));
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    @Transactional
    public Product updateProduct(
            Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateName(name, allowKakao);
        final Product product =
                productRepository.findById(id).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
        final Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다."));
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private void validateName(String name, boolean allowKakao) {
        final List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
