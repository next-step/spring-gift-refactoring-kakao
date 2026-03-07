package gift.service;

import gift.common.NameValidator;
import gift.model.Category;
import gift.model.Product;
import gift.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService {
    private static final NameValidator NAME_VALIDATOR = NameValidator.of("Product name",
        NameValidator.maxLength(15),
        NameValidator.allowedCharacters(),
        NameValidator.noKakao()
    );

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    public Page<Product> findAll(Long categoryId, Pageable pageable) {
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId, pageable);
        }
        return productRepository.findAll(pageable);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Product not found. id=" + id));
    }

    public Product create(String name, int price, String imageUrl, Long categoryId) {
        NAME_VALIDATOR.validateOrThrow(name);
        Category category = categoryService.findById(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    public Product update(Long id, String name, int price, String imageUrl, Long categoryId) {
        NAME_VALIDATOR.validateOrThrow(name);
        Category category = categoryService.findById(categoryId);
        Product product = findById(id);
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

}
