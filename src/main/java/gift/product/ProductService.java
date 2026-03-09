package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Category category = findCategory(request.categoryId());
        return productRepository.save(request.toEntity(category));
    }

    @Transactional
    public Product create(String name, int price, String imageUrl, Long categoryId) {
        Category category = findCategory(categoryId);
        return productRepository.save(Product.withKakaoAllowed(name, price, imageUrl, category));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Category category = findCategory(request.categoryId());
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, String name, int price, String imageUrl, Long categoryId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
        Category category = findCategory(categoryId);
        product.updateWithKakaoAllowed(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
    }

}
