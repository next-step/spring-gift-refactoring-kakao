package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.common.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
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
        return productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        Category category = findCategory(request.categoryId());
        return productRepository.save(request.toEntity(category));
    }

    @Transactional
    public Product createProduct(String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        Category category = findCategory(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category, allowKakao));
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        return updateProduct(id, request.name(), request.price(), request.imageUrl(), request.categoryId(), false);
    }

    @Transactional
    public Product updateProduct(Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        Category category = findCategory(categoryId);
        Product product = getProduct(id);
        product.update(name, price, imageUrl, category, allowKakao);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException("카테고리가 존재하지 않습니다."));
    }
}
