package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
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

    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Product create(String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        ProductNameValidator.validateOrThrow(name, allowKakao);
        Category category = categoryService.findById(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    @Transactional
    public Product update(Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        ProductNameValidator.validateOrThrow(name, allowKakao);
        Category category = categoryService.findById(categoryId);
        Product product = findById(id);
        product.update(name, price, imageUrl, category);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
